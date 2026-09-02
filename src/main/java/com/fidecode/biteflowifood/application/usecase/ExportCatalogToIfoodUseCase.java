package com.fidecode.biteflowifood.application.usecase;

import com.fidecode.biteflowifood.application.dto.biteflow.BiteflowCatalogResponse.*;
import com.fidecode.biteflowifood.application.dto.catalog.IfoodCatalogRequest.*;
import com.fidecode.biteflowifood.application.port.BiteflowCatalogClient;
import com.fidecode.biteflowifood.application.port.IfoodCatalogSyncClient;
import com.fidecode.biteflowifood.domain.catalog.entity.CatalogMapping;
import com.fidecode.biteflowifood.domain.catalog.repository.CatalogMappingRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class ExportCatalogToIfoodUseCase {

    @Inject
    BiteflowCatalogClient biteflowCatalogClient;

    @Inject
    IfoodCatalogSyncClient ifoodCatalogSyncClient;

    @Inject
    CatalogMappingRepository mappingRepository;

    public void execute(UUID storeId, String merchantId, String token) {
        // 1. Obter todas as categorias do BiteFlow
        List<CategoryResponse> categories = biteflowCatalogClient.listCategories(storeId);
        List<ModifierGroupResponse> modifierGroups = biteflowCatalogClient.listModifierGroups(storeId);

        for (CategoryResponse category : categories) {
            // Verificar se a categoria já está mapeada
            Optional<CatalogMapping> existingCatMapping = mappingRepository.findByBiteflowIdAndStoreId(UUID.fromString(category.id()), storeId);
            String ifoodCategoryId;

            if (existingCatMapping.isEmpty()) {
                // Criar no iFood
                IfoodCreateCategoryRequest catReq = CatalogExportMapper.toIfoodCategory(category);
                ifoodCategoryId = ifoodCatalogSyncClient.createCategory(token, merchantId, catReq);

                mappingRepository.save(new CatalogMapping(storeId, ifoodCategoryId, UUID.fromString(category.id()), "CATEGORY"));
            } else {
                ifoodCategoryId = existingCatMapping.get().getIfoodId();
            }

            // 2. Para cada produto da categoria
            List<ProductResponse> products = biteflowCatalogClient.listProducts(storeId, category.id());
            
            for (ProductResponse product : products) {
                Optional<CatalogMapping> existingProdMapping = mappingRepository.findByBiteflowIdAndStoreId(UUID.fromString(product.id()), storeId);
                
                if (existingProdMapping.isEmpty()) {
                    // Mapeia os modifiers do produto
                    List<ModifierGroupResponse> productGroups = modifierGroups.stream()
                            .filter(g -> product.modifierGroupIds() != null && product.modifierGroupIds().contains(g.id()))
                            .collect(Collectors.toList());

                    // TODO: Mapear id da opção para id do iFood Option
                    
                    IfoodCreateItemRequest itemReq = CatalogExportMapper.toIfoodItem(product, productGroups);
                    
                    // Adiciona o categoryId que acabamos de pegar
                    itemReq = new IfoodCreateItemRequest(
                            itemReq.type(),
                            ifoodCategoryId,
                            itemReq.name(),
                            itemReq.description(),
                            itemReq.externalCode(),
                            itemReq.status(),
                            itemReq.price(),
                            itemReq.optionGroups(),
                            itemReq.options()
                    );
                    
                    String ifoodItemId = ifoodCatalogSyncClient.createItem(token, merchantId, itemReq);

                    mappingRepository.save(new CatalogMapping(storeId, ifoodItemId, UUID.fromString(product.id()), "PRODUCT"));
                } else {
                    // Já existe. Em um cenário real de export, talvez pudesse fazer UPDATE (PATCH) dos dados e do status
                    String ifoodItemId = existingProdMapping.get().getIfoodId();
                    
                    ifoodCatalogSyncClient.updateItemStatus(token, merchantId, ifoodItemId, new IfoodUpdateStatusRequest(product.status()));
                    
                    // TODO: Update Price if it differs
                }
            }
        }
    }
}
