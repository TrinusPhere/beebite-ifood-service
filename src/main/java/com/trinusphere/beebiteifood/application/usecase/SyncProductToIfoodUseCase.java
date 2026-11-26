package com.trinusphere.beebiteifood.application.usecase;

import com.trinusphere.beebiteifood.application.dto.BiteflowCatalogChangedEvent;
import com.trinusphere.beebiteifood.application.dto.biteflow.BiteflowCatalogResponse.ChannelPriceDto;
import com.trinusphere.beebiteifood.application.dto.biteflow.BiteflowCatalogResponse.ProductResponse;
import com.trinusphere.beebiteifood.application.dto.catalog.IfoodCatalogRequest.IfoodUpdatePriceRequest;
import com.trinusphere.beebiteifood.application.dto.catalog.IfoodCatalogRequest.IfoodUpdateStatusRequest;
import com.trinusphere.beebiteifood.application.port.BiteflowCatalogClient;
import com.trinusphere.beebiteifood.application.port.IfoodCatalogSyncClient;
import com.trinusphere.beebiteifood.domain.catalog.entity.CatalogMapping;
import com.trinusphere.beebiteifood.domain.catalog.repository.CatalogMappingRepository;
import com.trinusphere.beebiteifood.domain.credential.entity.IfoodCredential;
import com.trinusphere.beebiteifood.domain.credential.repository.IfoodCredentialRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

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
        LOG.infof("[SYNC-TO-IFOOD] Processando evento: %s para o produto %s (catalog=%s, store=%s)",
                event.changeType(), event.productId(), event.catalogId(), event.storeId());

        CatalogMapping mapping = resolveProductMapping(event);
        if (mapping == null || mapping.getIfoodId() == null) {
            LOG.infof("[SYNC-TO-IFOOD] Produto %s não mapeado ou não sincronizado com o iFood. Ignorando.", event.productId());
            return;
        }

        IfoodCredential credential = resolveCredential(event, mapping);
        boolean credentialValid = credential != null && credential.isEnabled() && credential.isTokenValid();

        if ("PRODUCT_DELETED".equals(event.changeType())) {
            handleProductDeleted(event, mapping, credentialValid ? credential : null);
            return;
        }

        if (!credentialValid) {
            LOG.warnf("[SYNC-TO-IFOOD] Catálogo/Loja (%s / %s) sem credencial iFood válida. Ignorando.",
                    event.catalogId(), event.storeId());
            return;
        }

        String token = credential.getAccessToken();
        String merchantId = credential.getMerchantId();
        UUID apiStoreId = credential.getStoreId() != null ? credential.getStoreId() : event.storeId();

        BigDecimal effectiveIfoodPrice = event.effectiveIfoodPrice();
        String globalStatus = event.globalStatus();

        if (effectiveIfoodPrice == null || globalStatus == null) {
            ProductResponse product = biteflowCatalogClient.getProduct(apiStoreId, event.productId());
            if (product == null) {
                LOG.warnf("[SYNC-TO-IFOOD] Produto %s não encontrado no BiteFlow (HTTP 404). Ignorando.", event.productId());
                return;
            }
            if (effectiveIfoodPrice == null) {
                effectiveIfoodPrice = extractIfoodPrice(product);
            }
            if (globalStatus == null) {
                globalStatus = product.globalStatus() != null ? product.globalStatus() : product.status();
            }
        }

        if (effectiveIfoodPrice != null) {
            try {
                ifoodCatalogSyncClient.updateProductPrice(token, merchantId, mapping.getIfoodId(),
                        new IfoodUpdatePriceRequest(effectiveIfoodPrice));
                LOG.infof("[SYNC-TO-IFOOD] Preço iFood atualizado (%s) para produto %s", effectiveIfoodPrice, event.productId());
            } catch (Exception e) {
                LOG.errorf(e, "[SYNC-TO-IFOOD] Erro ao atualizar preço: %s", e.getMessage());
            }
        }

        if (globalStatus != null) {
            String ifoodStatus = "AVAILABLE".equals(globalStatus) ? "AVAILABLE" : "UNAVAILABLE";
            try {
                ifoodCatalogSyncClient.updateProductStatus(token, merchantId, mapping.getIfoodId(),
                        new IfoodUpdateStatusRequest(ifoodStatus));
                LOG.infof("[SYNC-TO-IFOOD] Status atualizado (%s) no iFood para produto %s", ifoodStatus, event.productId());
            } catch (Exception e) {
                LOG.errorf(e, "[SYNC-TO-IFOOD] Erro ao atualizar status: %s", e.getMessage());
            }
        }
    }

    private CatalogMapping resolveProductMapping(BiteflowCatalogChangedEvent event) {
        if (event.catalogId() != null) {
            Optional<CatalogMapping> byCatalog = catalogMappingRepository
                    .findByBiteflowIdAndCatalogId(event.productId(), event.catalogId());
            if (byCatalog.isPresent()) {
                return byCatalog.get();
            }
            var catalogList = catalogMappingRepository.findByCatalogId(event.catalogId());
            if (catalogList != null && !catalogList.isEmpty()) {
                CatalogMapping found = catalogList.stream()
                        .filter(m -> m.getBiteflowId() != null && m.getBiteflowId().equals(event.productId()))
                        .findFirst()
                        .orElse(null);
                if (found != null) {
                    return found;
                }
            }
        }

        if (event.storeId() != null) {
            var storeMappings = catalogMappingRepository.findByStoreId(event.storeId());
            if (storeMappings != null) {
                return storeMappings.stream()
                        .filter(m -> m.getBiteflowId() != null && m.getBiteflowId().equals(event.productId()))
                        .findFirst()
                        .orElse(null);
            }
        }
        return null;
    }

    private IfoodCredential resolveCredential(BiteflowCatalogChangedEvent event, CatalogMapping mapping) {
        UUID lookupCatalogId = event.catalogId() != null ? event.catalogId() : mapping.getCatalogId();
        if (lookupCatalogId != null) {
            Optional<IfoodCredential> byCatalog = credentialRepository.findByCatalogId(lookupCatalogId);
            if (byCatalog.isPresent()) {
                return byCatalog.get();
            }
        }
        if (event.storeId() != null) {
            Optional<IfoodCredential> byEventStore = credentialRepository.findByStoreId(event.storeId());
            if (byEventStore.isPresent()) {
                return byEventStore.get();
            }
        }
        if (mapping.getStoreId() != null && !mapping.getStoreId().equals(event.storeId())) {
            return credentialRepository.findByStoreId(mapping.getStoreId()).orElse(null);
        }
        return null;
    }

    private BigDecimal extractIfoodPrice(ProductResponse product) {
        if (product.channelPrices() != null) {
            for (ChannelPriceDto cp : product.channelPrices()) {
                if ("IFOOD".equalsIgnoreCase(cp.channel()) && cp.price() != null) {
                    return cp.price();
                }
            }
        }
        return product.price();
    }

    private void handleProductDeleted(BiteflowCatalogChangedEvent event, CatalogMapping mapping, IfoodCredential credential) {
        if (credential != null) {
            try {
                ifoodCatalogSyncClient.updateProductStatus(credential.getAccessToken(), credential.getMerchantId(),
                        mapping.getIfoodId(), new IfoodUpdateStatusRequest("UNAVAILABLE"));
                LOG.infof("[SYNC-TO-IFOOD] Produto %s pausado no iFood devido a exclusão no PDV", event.productId());
            } catch (Exception e) {
                LOG.errorf(e, "[SYNC-TO-IFOOD] Não foi possível pausar o item %s no iFood: %s", mapping.getIfoodId(), e.getMessage());
            }
        } else {
            LOG.warnf("[SYNC-TO-IFOOD] Catálogo/Loja %s sem credencial válida: item %s não foi pausado no iFood.",
                    event.catalogId() != null ? event.catalogId() : event.storeId(), mapping.getIfoodId());
        }

        catalogMappingRepository.delete(mapping);
        LOG.infof("[SYNC-TO-IFOOD] Vínculo iFood %s -> BiteFlow %s removido.", mapping.getIfoodId(), event.productId());
    }
}
