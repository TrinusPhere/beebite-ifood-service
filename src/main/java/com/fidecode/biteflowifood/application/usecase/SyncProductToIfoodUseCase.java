package com.fidecode.biteflowifood.application.usecase;

import com.fidecode.biteflowifood.application.dto.BiteflowCatalogChangedEvent;
import com.fidecode.biteflowifood.application.dto.biteflow.BiteflowCatalogResponse.ProductResponse;
import com.fidecode.biteflowifood.application.dto.catalog.IfoodCatalogRequest.IfoodUpdatePriceRequest;
import com.fidecode.biteflowifood.application.dto.catalog.IfoodCatalogRequest.IfoodUpdateStatusRequest;
import com.fidecode.biteflowifood.application.port.BiteflowCatalogClient;
import com.fidecode.biteflowifood.application.port.IfoodCatalogSyncClient;
import com.fidecode.biteflowifood.domain.catalog.entity.CatalogMapping;
import com.fidecode.biteflowifood.domain.catalog.repository.CatalogMappingRepository;
import com.fidecode.biteflowifood.domain.credential.entity.IfoodCredential;
import com.fidecode.biteflowifood.domain.credential.repository.IfoodCredentialRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

@ApplicationScoped
public class SyncProductToIfoodUseCase {

    private static final Logger LOG = Logger.getLogger(SyncProductToIfoodUseCase.class);

    private final CatalogMappingRepository catalogMappingRepository;
    private final BiteflowCatalogClient biteflowCatalogClient;
    private final IfoodCatalogSyncClient ifoodCatalogSyncClient;
    private final IfoodCredentialRepository credentialRepository;

    @Inject
    public SyncProductToIfoodUseCase(CatalogMappingRepository catalogMappingRepository,
                                     BiteflowCatalogClient biteflowCatalogClient,
                                     IfoodCatalogSyncClient ifoodCatalogSyncClient,
                                     IfoodCredentialRepository credentialRepository) {
        this.catalogMappingRepository = catalogMappingRepository;
        this.biteflowCatalogClient = biteflowCatalogClient;
        this.ifoodCatalogSyncClient = ifoodCatalogSyncClient;
        this.credentialRepository = credentialRepository;
    }

    @jakarta.enterprise.context.control.ActivateRequestContext
    @jakarta.transaction.Transactional
    public void execute(BiteflowCatalogChangedEvent event) {
        LOG.infof("[SYNC-TO-IFOOD] Processando evento: %s para o produto %s", event.changeType(), event.productId());

        var mappings = catalogMappingRepository.findByStoreId(event.storeId());
        CatalogMapping mapping = mappings.stream()
                .filter(m -> m.getBiteflowId() != null && m.getBiteflowId().equals(event.productId()))
                .findFirst()
                .orElse(null);

        if (mapping == null || mapping.getIfoodId() == null) {
            LOG.infof("[SYNC-TO-IFOOD] Produto %s não mapeado ou não sincronizado com o iFood. Ignorando.", event.productId());
            return;
        }

        IfoodCredential credential = credentialRepository.findByStoreId(event.storeId()).orElse(null);
        boolean credentialValid = credential != null && credential.isEnabled() && credential.isTokenValid();

        if ("PRODUCT_DELETED".equals(event.changeType())) {
            handleProductDeleted(event, mapping, credentialValid ? credential : null);
            return;
        }

        if (!credentialValid) {
            LOG.warnf("[SYNC-TO-IFOOD] Loja %s sem credencial válida. Ignorando.", event.storeId());
            return;
        }

        String token = credential.getAccessToken();
        String merchantId = credential.getMerchantId();

        ProductResponse product = biteflowCatalogClient.getProduct(event.storeId(), event.productId());
        if (product == null) {
            LOG.warnf("[SYNC-TO-IFOOD] Produto %s não encontrado no BiteFlow (HTTP 404). Ignorando.", event.productId());
            return;
        }

        // Atualizar preço
        if (product.price() != null) {
            try {
                ifoodCatalogSyncClient.updateProductPrice(token, merchantId, mapping.getIfoodId(),
                        new IfoodUpdatePriceRequest(product.price()));
                LOG.infof("[SYNC-TO-IFOOD] Preço atualizado no iFood para produto %s", event.productId());
            } catch (Exception e) {
                LOG.errorf(e, "[SYNC-TO-IFOOD] Erro ao atualizar preço: %s", e.getMessage());
            }
        }

        // Atualizar status
        if (product.status() != null) {
            String ifoodStatus = "AVAILABLE".equals(product.status()) ? "AVAILABLE" : "UNAVAILABLE";
            try {
                ifoodCatalogSyncClient.updateProductStatus(token, merchantId, mapping.getIfoodId(),
                        new IfoodUpdateStatusRequest(ifoodStatus));
                LOG.infof("[SYNC-TO-IFOOD] Status atualizado (%s) no iFood para produto %s", ifoodStatus, event.productId());
            } catch (Exception e) {
                LOG.errorf(e, "[SYNC-TO-IFOOD] Erro ao atualizar status: %s", e.getMessage());
            }
        }
    }

    /**
     * Produto excluído no PDV: pausa o item no iFood (se possível) e remove o vínculo,
     * evitando mapeamentos órfãos que quebram o merge/comparação depois.
     */
    private void handleProductDeleted(BiteflowCatalogChangedEvent event, CatalogMapping mapping, IfoodCredential credential) {
        if (credential != null) {
            try {
                ifoodCatalogSyncClient.updateProductStatus(credential.getAccessToken(), credential.getMerchantId(),
                        mapping.getIfoodId(), new IfoodUpdateStatusRequest("UNAVAILABLE"));
                LOG.infof("[SYNC-TO-IFOOD] Produto %s pausado no iFood devido a exclusão no PDV", event.productId());
            } catch (Exception e) {
                // O subscriber faz ACK de erros de negócio (sem retry). Por isso o vínculo é removido mesmo assim:
                // o item aparecerá como "apenas no iFood" na comparação, em vez de virar um vínculo órfão.
                LOG.errorf(e, "[SYNC-TO-IFOOD] Não foi possível pausar o item %s no iFood: %s", mapping.getIfoodId(), e.getMessage());
            }
        } else {
            LOG.warnf("[SYNC-TO-IFOOD] Loja %s sem credencial válida: item %s não foi pausado no iFood.",
                    event.storeId(), mapping.getIfoodId());
        }

        catalogMappingRepository.delete(mapping);
        LOG.infof("[SYNC-TO-IFOOD] Vínculo iFood %s -> BiteFlow %s removido.", mapping.getIfoodId(), event.productId());
    }
}
