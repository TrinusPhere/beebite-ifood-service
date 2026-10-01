package com.fidecode.biteflowifood.application.usecase;

import com.fidecode.biteflowifood.application.dto.biteflow.BiteflowCatalogResponse.CategoryResponse;
import com.fidecode.biteflowifood.application.dto.biteflow.BiteflowCatalogResponse.ProductResponse;
import com.fidecode.biteflowifood.application.dto.catalog.CatalogDiffResponse;
import com.fidecode.biteflowifood.application.dto.catalog.CatalogDiffResponse.CatalogDiffEntry;
import com.fidecode.biteflowifood.application.dto.catalog.IfoodCatalogResponse.IfoodCategory;
import com.fidecode.biteflowifood.application.dto.catalog.IfoodCatalogResponse.IfoodItemFull;
import com.fidecode.biteflowifood.application.port.BiteflowCatalogClient;
import com.fidecode.biteflowifood.application.port.IfoodCatalogClient;
import com.fidecode.biteflowifood.domain.catalog.entity.CatalogMapping;
import com.fidecode.biteflowifood.domain.catalog.repository.CatalogMappingRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import com.fidecode.biteflowifood.application.dto.catalog.IfoodCatalogResponse.IfoodPrice;

import com.fidecode.biteflowifood.application.dto.catalog.CatalogDiffResponse.IfoodItemPresentation;
import com.fasterxml.jackson.databind.JsonNode;

@ApplicationScoped
public class CompareCatalogUseCase {

    @Inject
    IfoodCatalogClient ifoodCatalogClient;

    @Inject
    BiteflowCatalogClient biteflowCatalogClient;

    @Inject
    CatalogMappingRepository mappingRepository;

    public CatalogDiffResponse execute(UUID storeId, String merchantId, String token) {
        List<CatalogDiffEntry> entries = new ArrayList<>();
        int matches = 0, conflicts = 0, biteflowOnly = 0, ifoodOnly = 0;

        // 1. Obter catálogo do BiteFlow
        List<CategoryResponse> bfCategories = biteflowCatalogClient.listCategories(storeId);
        List<ProductResponse> bfProducts = new ArrayList<>();
        for (CategoryResponse cat : bfCategories) {
            bfProducts.addAll(biteflowCatalogClient.listProducts(storeId, cat.id()));
        }
        
        Map<String, ProductResponse> bfProductMap = bfProducts.stream()
                .collect(Collectors.toMap(ProductResponse::id, p -> p));

        // 2. Obter catálogo do iFood
        List<com.fidecode.biteflowifood.application.dto.catalog.IfoodCatalogResponse.IfoodItemData> sellableItems = ifoodCatalogClient.listSellableItems(token, merchantId);

        try {
            java.nio.file.Files.writeString(java.nio.file.Paths.get("sellable_items_dump.json"), new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(sellableItems));
        } catch (Exception e) {
            e.printStackTrace();
        }

        Map<String, IfoodPrice> sellablePrices = sellableItems.stream()
                .filter(i -> extractPrice(i) != null)
                .collect(Collectors.toMap(i -> i.id() != null ? i.id() : i.itemId(), this::extractPrice, (p1, p2) -> p1));

        List<IfoodCategory> ifoodCategories = ifoodCatalogClient.listCategories(token, merchantId);
        List<IfoodItemFull> ifoodItems = new ArrayList<>();

        for (IfoodCategory cat : ifoodCategories) {
            var categoryItems = ifoodCatalogClient.listCategoryItems(token, merchantId, cat.id());
            if (categoryItems == null || categoryItems.items() == null) continue;
            
            for (var itemData : categoryItems.items()) {
                var itemProducts = categoryItems.products() != null ? categoryItems.products().stream().filter(p -> p.id().equals(itemData.id()) || p.id().equals(itemData.productId())).collect(Collectors.toList()) : List.<com.fidecode.biteflowifood.application.dto.catalog.IfoodCatalogResponse.IfoodProduct>of();
                ifoodItems.add(new IfoodItemFull(itemData, itemProducts, List.of(), List.of())); // For comparison, we usually only need the base item info
            }
        }

        // 3. Obter mappings existentes
        List<CatalogMapping> mappings = mappingRepository.findByStoreId(storeId).stream()
                .filter(m -> m.getMappingType().equals("PRODUCT") && m.getIfoodId() != null && !m.getIfoodId().isBlank())
                .collect(Collectors.toList());
        Map<String, String> ifoodToBf = mappings.stream()
                .collect(Collectors.toMap(CatalogMapping::getIfoodId, m -> m.getBiteflowId().toString(), (v1, v2) -> v1));
        Map<String, String> bfToIfood = mappings.stream()
                .collect(Collectors.toMap(m -> m.getBiteflowId().toString(), CatalogMapping::getIfoodId, (v1, v2) -> v1));

        Set<String> processedBfIds = new HashSet<>();

        // 4. Analisar itens do iFood
        for (IfoodItemFull item : ifoodItems) {
            String ifoodId = item.item().id();
            String matchedBfId = ifoodToBf.get(ifoodId);

            var ifoodProduct = item.products() != null && !item.products().isEmpty() ? item.products().get(0) : null;
            String ifoodProductName = ifoodProduct != null ? ifoodProduct.name() : "Produto sem nome";

            // Tentar match por externalCode ou nome exato se não houver mapeamento
            if (matchedBfId == null) {
                matchedBfId = bfProducts.stream()
                        .filter(p -> (p.externalCode() != null && p.externalCode().equals(item.item().externalCode())) ||
                                     p.name().equalsIgnoreCase(ifoodProductName))
                        .map(ProductResponse::id)
                        .findFirst()
                        .orElse(null);
            }

            if (matchedBfId != null && bfProductMap.containsKey(matchedBfId)) {
                // Existe em ambos -> Verificar se há conflito
                ProductResponse bfProduct = bfProductMap.get(matchedBfId);
                processedBfIds.add(matchedBfId);
                
                List<String> conflictFields = new ArrayList<>();
                
                // Comparar Preço
                BigDecimal bfPrice = bfProduct.channelPrices() != null ? 
                        bfProduct.channelPrices().stream().filter(c -> "IFOOD".equals(c.channel())).map(c -> c.price()).findFirst().orElse(bfProduct.price())
                        : bfProduct.price();
                if (bfPrice == null) bfPrice = BigDecimal.ZERO;
                        
                IfoodPrice currentIfoodPrice = extractPrice(item.item());
                if (currentIfoodPrice == null && sellablePrices.containsKey(ifoodId)) {
                    currentIfoodPrice = sellablePrices.get(ifoodId);
                }
                        
                BigDecimal ifoodPrice = currentIfoodPrice != null && currentIfoodPrice.value() != null ? currentIfoodPrice.value() : BigDecimal.ZERO;
                
                if (bfPrice.compareTo(ifoodPrice) != 0) {
                    conflictFields.add("price");
                }
                
                // Comparar Nome
                if (!bfProduct.name().equals(ifoodProductName)) {
                    conflictFields.add("name");
                }
                
                // Comparar Status
                String bfStatus = bfProduct.status() != null ? bfProduct.status() : "UNAVAILABLE";
                String ifStatus = item.item().status() != null ? item.item().status() : "UNAVAILABLE";
                if (!bfStatus.equals(ifStatus)) {
                    // Note: iFood only has AVAILABLE/UNAVAILABLE. PAUSED in BF means UNAVAILABLE in IFood.
                    if (!(bfStatus.equals("PAUSED") && ifStatus.equals("UNAVAILABLE"))) {
                        conflictFields.add("status");
                    }
                }

                IfoodItemPresentation presentation = new IfoodItemPresentation(
                        item.item().id(),
                        ifoodProductName,
                        currentIfoodPrice,
                        item.item().status()
                );
                
                if (conflictFields.isEmpty()) {
                    entries.add(new CatalogDiffEntry("MATCH", bfProduct, presentation, List.of()));
                    matches++;
                } else {
                    entries.add(new CatalogDiffEntry("CONFLICT", bfProduct, presentation, conflictFields));
                    conflicts++;
                }
            } else {
                IfoodPrice currentIfoodPrice = extractPrice(item.item());
                if (currentIfoodPrice == null && sellablePrices.containsKey(ifoodId)) {
                    currentIfoodPrice = sellablePrices.get(ifoodId);
                }

                IfoodItemPresentation presentation = new IfoodItemPresentation(
                        item.item().id(),
                        ifoodProductName,
                        currentIfoodPrice,
                        item.item().status()
                );
                // Apenas no iFood
                entries.add(new CatalogDiffEntry("IFOOD_ONLY", null, presentation, List.of()));
                ifoodOnly++;
            }
        }

        // 5. Analisar itens que sobraram no BiteFlow
        for (ProductResponse bfProduct : bfProducts) {
            if (!processedBfIds.contains(bfProduct.id())) {
                entries.add(new CatalogDiffEntry("BITEFLOW_ONLY", bfProduct, null, List.of()));
                biteflowOnly++;
            }
        }

        return new CatalogDiffResponse(entries, matches, conflicts, biteflowOnly, ifoodOnly);
    }

    private IfoodPrice extractPrice(com.fidecode.biteflowifood.application.dto.catalog.IfoodCatalogResponse.IfoodItemData itemData) {
        if (itemData.price() != null) return itemData.price();
        
        if (itemData.contextModifiers() != null && itemData.contextModifiers().isArray()) {
            for (JsonNode modifier : itemData.contextModifiers()) {
                String ctx = modifier.path("catalogContext").asText();
                if ("DEFAULT".equals(ctx) || "IFOOD".equals(ctx) || ctx.isBlank()) {
                    JsonNode priceNode = modifier.path("price");
                    if (!priceNode.isMissingNode() && !priceNode.isNull()) {
                        BigDecimal value = priceNode.path("value").isMissingNode() ? null : new BigDecimal(priceNode.path("value").asText());
                        BigDecimal originalValue = priceNode.path("originalValue").isMissingNode() ? null : new BigDecimal(priceNode.path("originalValue").asText());
                        return new IfoodPrice(value, originalValue);
                    }
                }
            }
        }
        return null;
    }
}
