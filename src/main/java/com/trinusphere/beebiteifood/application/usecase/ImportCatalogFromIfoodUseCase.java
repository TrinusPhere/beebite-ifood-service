package com.trinusphere.beebiteifood.application.usecase;

import com.trinusphere.beebiteifood.application.dto.biteflow.BiteflowCatalogRequest.*;
import com.trinusphere.beebiteifood.application.dto.catalog.IfoodCatalogResponse.*;
import com.trinusphere.beebiteifood.application.port.BiteflowCatalogClient;
import com.trinusphere.beebiteifood.application.port.IfoodCatalogClient;
import com.trinusphere.beebiteifood.application.port.ImageStoragePort;
import com.trinusphere.beebiteifood.domain.catalog.entity.CatalogMapping;
import com.trinusphere.beebiteifood.domain.catalog.repository.CatalogMappingRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

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

    @Transactional
    public void execute(UUID storeId, String merchantId, String token) {
        execute(storeId, storeId, merchantId, token);
    }

    @Transactional
    public void execute(UUID storeId, UUID catalogId, String merchantId, String token) {
        UUID effectiveCatalogId = catalogId != null ? catalogId : storeId;
        List<IfoodCategory> categories = ifoodCatalogClient.listCategories(token, merchantId);

        for (IfoodCategory category : categories) {
            Optional<CatalogMapping> existingCatMapping = findMappingByIfoodId(category.id(), effectiveCatalogId, storeId);
            UUID biteflowCategoryId;

            if (existingCatMapping.isEmpty()) {
                CreateCategoryRequest catReq = CatalogSyncMapper.toCreateCategoryRequest(category);
                String createdCatId = biteflowCatalogClient.createCategory(storeId, catReq);
                biteflowCategoryId = UUID.fromString(createdCatId.replace("\"", ""));

                mappingRepository.save(new CatalogMapping(effectiveCatalogId, storeId, category.id(), biteflowCategoryId, "CATEGORY"));
            } else {
                biteflowCategoryId = existingCatMapping.get().getBiteflowId();
            }

            IfoodCategoryItemsResponse categoryItems = ifoodCatalogClient.listCategoryItems(token, merchantId, category.id());
            if (categoryItems == null || categoryItems.items() == null) continue;

            for (IfoodItemData itemData : categoryItems.items()) {
                Optional<CatalogMapping> existingProdMapping = findMappingByIfoodId(itemData.id(), effectiveCatalogId, storeId);

                if (existingProdMapping.isEmpty()) {
                    List<IfoodProduct> itemProducts = categoryItems.products() != null
                            ? categoryItems.products().stream()
                            .filter(p -> p.id().equals(itemData.id()) || p.id().equals(itemData.productId()))
                            .toList()
                            : List.of();

                    List<IfoodOptionGroup> itemOptionGroups = new java.util.ArrayList<>();
                    if (!itemProducts.isEmpty() && categoryItems.optionGroups() != null) {
                        com.fasterxml.jackson.databind.JsonNode optionGroupsNode = itemProducts.get(0).optionGroups();
                        if (optionGroupsNode != null && optionGroupsNode.isArray()) {
                            java.util.Set<String> groupIds = new java.util.HashSet<>();
                            for (com.fasterxml.jackson.databind.JsonNode node : optionGroupsNode) {
                                groupIds.add(node.asText());
                            }
                            itemOptionGroups = categoryItems.optionGroups().stream().filter(g -> groupIds.contains(g.id())).toList();
                        } else if (optionGroupsNode != null && optionGroupsNode.isTextual() && !optionGroupsNode.asText().isEmpty()) {
                            itemOptionGroups = categoryItems.optionGroups().stream().filter(g -> g.id().equals(optionGroupsNode.asText())).toList();
                        }
                    }

                    List<IfoodOption> itemOptions = new java.util.ArrayList<>();
                    if (categoryItems.options() != null && !itemOptionGroups.isEmpty()) {
                        java.util.Set<String> optionIds = new java.util.HashSet<>();
                        for (IfoodOptionGroup group : itemOptionGroups) {
                            if (group.optionIds() != null && group.optionIds().isArray()) {
                                for (com.fasterxml.jackson.databind.JsonNode node : group.optionIds()) {
                                    optionIds.add(node.asText());
                                }
                            } else if (group.optionIds() != null && group.optionIds().isTextual() && !group.optionIds().asText().isEmpty()) {
                                optionIds.add(group.optionIds().asText());
                            }
                        }
                        itemOptions = categoryItems.options().stream().filter(o -> optionIds.contains(o.id())).toList();
                    }

                    IfoodItemFull item = new IfoodItemFull(itemData, itemProducts, itemOptionGroups, itemOptions);
                    List<String> modifierGroupIds = processModifierGroups(storeId, effectiveCatalogId, item);

                    IfoodProduct prod = item.products() != null && !item.products().isEmpty() ? item.products().get(0) : null;
                    String localImageUrl = null;
                    if (prod != null && prod.imagePath() != null) {
                        localImageUrl = imageStoragePort.storeImageFromUrl(prod.imagePath());
                    }

                    CreateProductRequest baseProdReq = CatalogSyncMapper.toCreateProductRequest(item, null);
                    CreateProductRequest finalProdReq = new CreateProductRequest(
                            baseProdReq.name(),
                            biteflowCategoryId.toString(),
                            baseProdReq.description(),
                            baseProdReq.price(),
                            null,
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

                    mappingRepository.save(new CatalogMapping(effectiveCatalogId, storeId, item.item().id(), biteflowProductId, "PRODUCT"));
                }
            }
        }
    }

    private List<String> processModifierGroups(UUID storeId, UUID catalogId, IfoodItemFull item) {
        if (item.optionGroups() == null || item.optionGroups().isEmpty()) {
            return List.of();
        }

        return item.optionGroups().stream().map(group -> {
            Optional<CatalogMapping> existingGroupMapping = findMappingByIfoodId(group.id(), catalogId, storeId);
            if (existingGroupMapping.isPresent()) {
                return existingGroupMapping.get().getBiteflowId().toString();
            }

            CreateModifierGroupRequest groupReq = CatalogSyncMapper.toCreateModifierGroupRequest(group, item);
            String createdGroupId = biteflowCatalogClient.createModifierGroup(storeId, groupReq);
            UUID biteflowGroupId = UUID.fromString(createdGroupId.replace("\"", ""));

            mappingRepository.save(new CatalogMapping(catalogId, storeId, group.id(), biteflowGroupId, "MODIFIER_GROUP"));
            return biteflowGroupId.toString();
        }).collect(Collectors.toList());
    }

    private Optional<CatalogMapping> findMappingByIfoodId(String ifoodId, UUID catalogId, UUID storeId) {
        Optional<CatalogMapping> byCatalog = mappingRepository.findByIfoodIdAndCatalogId(ifoodId, catalogId);
        return byCatalog.isPresent() ? byCatalog : mappingRepository.findByIfoodIdAndStoreId(ifoodId, storeId);
    }
}
