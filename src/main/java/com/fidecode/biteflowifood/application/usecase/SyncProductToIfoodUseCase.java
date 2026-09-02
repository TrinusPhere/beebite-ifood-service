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
        if (credential == null || !credential.isEnabled() || !credential.isTokenValid()) {
            LOG.warnf("[SYNC-TO-IFOOD] Loja %s sem credencial válida. Ignorando.", event.storeId());
            return;
        }

        String token = credential.getAccessToken();
        String merchantId = credential.getMerchantId();

        if ("PRODUCT_DELETED".equals(event.changeType())) {
            // Se foi deletado no PDV, vamos pausar no iFood
            ifoodCatalogSyncClient.updateItemStatus(token, merchantId, mapping.getIfoodId(),
                    new IfoodUpdateStatusRequest("UNAVAILABLE")); // assumindo UNAVAILABLE
            LOG.infof("[SYNC-TO-IFOOD] Produto %s pausado no iFood devido a exclusão no PDV", event.productId());
            return;
        }

        ProductResponse product = biteflowCatalogClient.getProduct(event.storeId(), event.productId());
        if (product == null) {
            LOG.warnf("[SYNC-TO-IFOOD] Produto %s não encontrado no BiteFlow (HTTP 404). Ignorando.", event.productId());
            return;
        }

        // Atualizar preço
        if (product.price() != null) {
            try {
                ifoodCatalogSyncClient.updateItemPrice(token, merchantId, mapping.getIfoodId(),
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
                ifoodCatalogSyncClient.updateItemStatus(token, merchantId, mapping.getIfoodId(),
                        new IfoodUpdateStatusRequest(ifoodStatus));
                LOG.infof("[SYNC-TO-IFOOD] Status atualizado (%s) no iFood para produto %s", ifoodStatus, event.productId());
            } catch (Exception e) {
                LOG.errorf(e, "[SYNC-TO-IFOOD] Erro ao atualizar status: %s", e.getMessage());
            }
        }
    }
}
