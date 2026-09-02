package com.fidecode.biteflowifood.application.usecase;

import com.fidecode.biteflowifood.application.dto.biteflow.BiteflowCatalogRequest.*;
import com.fidecode.biteflowifood.application.dto.catalog.IfoodCatalogResponse.*;
import com.fidecode.biteflowifood.application.port.BiteflowCatalogClient;
import com.fidecode.biteflowifood.application.port.IfoodCatalogClient;
import com.fidecode.biteflowifood.application.port.ImageStoragePort;
import com.fidecode.biteflowifood.domain.catalog.entity.CatalogMapping;
import com.fidecode.biteflowifood.domain.catalog.repository.CatalogMappingRepository;
import com.fidecode.biteflowifood.domain.shared.exception.BusinessRuleException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class ImportCatalogFromIfoodUseCase {

    @Inject
    IfoodCatalogClient ifoodCatalogClient;

    @Inject
    BiteflowCatalogClient biteflowCatalogClient;

    @Inject
    CatalogMappingRepository mappingRepository;

    @Inject
    ImageStoragePort imageStoragePort;

    public void execute(UUID storeId, String merchantId, String token) {
        // 1. Obtém as categorias do iFood
        List<IfoodCategory> categories = ifoodCatalogClient.listCategories(token, merchantId);

        for (IfoodCategory category : categories) {
            // Verifica se a categoria já foi importada
            Optional<CatalogMapping> existingCatMapping = mappingRepository.findByIfoodIdAndStoreId(category.id(), storeId);
            UUID biteflowCategoryId;

            if (existingCatMapping.isEmpty()) {
                // Traduz e salva no BiteFlow
                CreateCategoryRequest catReq = CatalogSyncMapper.toCreateCategoryRequest(category);
                String createdCatId = biteflowCatalogClient.createCategory(storeId, catReq);
                biteflowCategoryId = UUID.fromString(createdCatId.replace("\"", ""));

                mappingRepository.save(new CatalogMapping(storeId, category.id(), biteflowCategoryId, "CATEGORY"));
            } else {
                biteflowCategoryId = existingCatMapping.get().getBiteflowId();
            }

            // 2. Obtém os itens desta categoria
            List<IfoodItemFull> items = ifoodCatalogClient.listCategoryItems(token, merchantId, category.id());

            for (IfoodItemFull item : items) {
                Optional<CatalogMapping> existingProdMapping = mappingRepository.findByIfoodIdAndStoreId(item.item().id(), storeId);

                if (existingProdMapping.isEmpty()) {
                    // Processa Option Groups (Modificadores) primeiro
                    List<String> modifierGroupIds = processModifierGroups(storeId, merchantId, token, item);

                    // Processa Imagem
                    IfoodProduct prod = item.products() != null && !item.products().isEmpty() ? item.products().get(0) : null;
                    String localImageUrl = null;
                    if (prod != null && prod.imagePath() != null) {
                        localImageUrl = imageStoragePort.storeImageFromUrl(prod.imagePath());
                    }

                    // Traduz e salva o produto
                    CreateProductRequest baseProdReq = CatalogSyncMapper.toCreateProductRequest(item, null);
                    
                    CreateProductRequest finalProdReq = new CreateProductRequest(
                            baseProdReq.name(),
                            biteflowCategoryId.toString(),
                            baseProdReq.description(),
                            baseProdReq.price(),
                            localImageUrl,
                            baseProdReq.stock(),
                            baseProdReq.status(),
                            baseProdReq.externalCode(),
                            baseProdReq.catalogItemType(),
                            baseProdReq.channelPrices(),
                            baseProdReq.productType(),
                            baseProdReq.trackStock(),
                            baseProdReq.productionSectorId(),
                            baseProdReq.saleUnit(),
                            baseProdReq.availableDigitalMenu(),
                            baseProdReq.availablePdv(),
                            baseProdReq.technicalSheet(),
                            baseProdReq.removableItemIds(),
                            modifierGroupIds
                    );

                    String createdProdId = biteflowCatalogClient.createProduct(storeId, finalProdReq);
                    UUID biteflowProductId = UUID.fromString(createdProdId.replace("\"", ""));

                    mappingRepository.save(new CatalogMapping(storeId, item.item().id(), biteflowProductId, "PRODUCT"));
                }
            }
        }
    }

    private List<String> processModifierGroups(UUID storeId, String merchantId, String token, IfoodItemFull item) {
        if (item.optionGroups() == null || item.optionGroups().isEmpty()) {
            return List.of();
        }

        return item.optionGroups().stream().map(group -> {
            Optional<CatalogMapping> existingGroupMapping = mappingRepository.findByIfoodIdAndStoreId(group.id(), storeId);
            if (existingGroupMapping.isPresent()) {
                return existingGroupMapping.get().getBiteflowId().toString();
            }

            // Traduz Options
            CreateModifierGroupRequest groupReq = CatalogSyncMapper.toCreateModifierGroupRequest(group, item);
            
            // TODO: Tratar link de Option com Produto se a opção for um produto já existente

            String createdGroupId = biteflowCatalogClient.createModifierGroup(storeId, groupReq);
            UUID biteflowGroupId = UUID.fromString(createdGroupId.replace("\"", ""));

            mappingRepository.save(new CatalogMapping(storeId, group.id(), biteflowGroupId, "MODIFIER_GROUP"));
            return biteflowGroupId.toString();
        }).collect(Collectors.toList());
    }
}
