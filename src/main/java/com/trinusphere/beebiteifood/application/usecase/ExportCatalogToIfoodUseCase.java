package com.trinusphere.beebiteifood.application.usecase;

import com.trinusphere.beebiteifood.application.dto.biteflow.BiteflowCatalogResponse.*;
import com.trinusphere.beebiteifood.application.dto.catalog.IfoodCatalogRequest.*;
import com.trinusphere.beebiteifood.application.port.BiteflowCatalogClient;
import com.trinusphere.beebiteifood.application.port.IfoodCatalogSyncClient;
import com.trinusphere.beebiteifood.domain.catalog.entity.CatalogMapping;
import com.trinusphere.beebiteifood.domain.catalog.repository.CatalogMappingRepository;
import com.trinusphere.beebiteifood.domain.shared.exception.BusinessRuleException;
import com.trinusphere.beebiteifood.domain.shared.exception.IfoodIntegrationException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class ExportCatalogToIfoodUseCase {

    private static final Logger LOG = Logger.getLogger(ExportCatalogToIfoodUseCase.class);

    @Inject
    BiteflowCatalogClient biteflowCatalogClient;

    @Inject
    IfoodCatalogSyncClient ifoodCatalogSyncClient;

    @Inject
    CatalogMappingRepository mappingRepository;

    @jakarta.transaction.Transactional
    public void execute(UUID storeId, String merchantId, String token) {
        execute(storeId, storeId, merchantId, token);
    }

    @jakarta.transaction.Transactional
    public void execute(UUID storeId, UUID catalogId, String merchantId, String token) {
        UUID effectiveCatalogId = catalogId != null ? catalogId : storeId;
        LOG.infof("[EXPORT] Iniciando export para storeId=%s, catalogId=%s, merchantId=%s", storeId, effectiveCatalogId, merchantId);

        List<String> catalogIds = ifoodCatalogSyncClient.listCatalogIds(token, merchantId);
        if (catalogIds.isEmpty()) {
            throw new BusinessRuleException("Nenhum catálogo encontrado no iFood para o merchantId: " + merchantId);
        }
        String ifoodCatalogId = catalogIds.get(0);

        List<CategoryResponse> categories = biteflowCatalogClient.listCategories(storeId);
        List<ModifierGroupResponse> modifierGroups = biteflowCatalogClient.listModifierGroups(storeId);

        for (CategoryResponse category : categories) {
            UUID bfCatUuid = UUID.fromString(category.id());
            Optional<CatalogMapping> existingCatMapping = findMappingByBiteflowId(bfCatUuid, effectiveCatalogId, storeId);
            String ifoodCategoryId;

            if (existingCatMapping.isEmpty()) {
                IfoodCreateCategoryRequest catReq = CatalogExportMapper.toIfoodCategory(category);
                ifoodCategoryId = ifoodCatalogSyncClient.createCategory(token, merchantId, ifoodCatalogId, catReq);
                mappingRepository.save(new CatalogMapping(effectiveCatalogId, storeId, ifoodCategoryId, bfCatUuid, "CATEGORY"));
            } else {
                ifoodCategoryId = existingCatMapping.get().getIfoodId();
            }

            List<ProductResponse> products = biteflowCatalogClient.listProducts(storeId, category.id());
            for (ProductResponse product : products) {
                UUID bfProdUuid = UUID.fromString(product.id());
                Optional<CatalogMapping> existingProdMapping = findMappingByBiteflowId(bfProdUuid, effectiveCatalogId, storeId);

                if (existingProdMapping.isEmpty()) {
                    List<ModifierGroupResponse> productGroups = modifierGroups.stream()
                            .filter(g -> product.modifierGroupIds() != null && product.modifierGroupIds().contains(g.id()))
                            .collect(Collectors.toList());

                    String generatedItemId = UUID.randomUUID().toString();
                    IfoodCreateProductRequest productReq = CatalogExportMapper.toIfoodProduct(product, productGroups, ifoodCategoryId, generatedItemId);

                    ifoodCatalogSyncClient.createProduct(token, merchantId, productReq);
                    mappingRepository.save(new CatalogMapping(effectiveCatalogId, storeId, generatedItemId, bfProdUuid, "PRODUCT"));
                } else {
                    String ifoodProductId = existingProdMapping.get().getIfoodId();
                    String effectiveGlobalStatus = product.globalStatus() != null ? product.globalStatus() : product.status();
                    try {
                        ifoodCatalogSyncClient.updateProductStatus(token, merchantId, ifoodProductId,
                                new IfoodUpdateStatusRequest(effectiveGlobalStatus));
                    } catch (IfoodIntegrationException ex) {
                        if (ex.getMessage().contains("HTTP 404")) {
                            mappingRepository.delete(existingProdMapping.get());
                            throw new BusinessRuleException("O iFood não encontrou um produto mapeado (404). O mapeamento órfão foi removido. Execute o EXPORT novamente.");
                        } else {
                            throw ex;
                        }
                    }
                }
            }
        }
    }

    private Optional<CatalogMapping> findMappingByBiteflowId(UUID biteflowId, UUID catalogId, UUID storeId) {
        Optional<CatalogMapping> byCatalog = mappingRepository.findByBiteflowIdAndCatalogId(biteflowId, catalogId);
        return byCatalog.isPresent() ? byCatalog : mappingRepository.findByBiteflowIdAndStoreId(biteflowId, storeId);
    }
}
