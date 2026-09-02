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
        List<IfoodCategory> ifoodCategories = ifoodCatalogClient.listCategories(token, merchantId);
        List<IfoodItemFull> ifoodItems = new ArrayList<>();
        for (IfoodCategory cat : ifoodCategories) {
            ifoodItems.addAll(ifoodCatalogClient.listCategoryItems(token, merchantId, cat.id()));
        }

        // 3. Obter mappings existentes
        List<CatalogMapping> mappings = mappingRepository.findByStoreId(storeId).stream()
                .filter(m -> m.getMappingType().equals("PRODUCT"))
                .collect(Collectors.toList());
        Map<String, String> ifoodToBf = mappings.stream()
                .collect(Collectors.toMap(CatalogMapping::getIfoodId, m -> m.getBiteflowId().toString()));
        Map<String, String> bfToIfood = mappings.stream()
                .collect(Collectors.toMap(m -> m.getBiteflowId().toString(), CatalogMapping::getIfoodId));

        Set<String> processedBfIds = new HashSet<>();

        // 4. Analisar itens do iFood
        for (IfoodItemFull item : ifoodItems) {
            String ifoodId = item.item().id();
            String matchedBfId = ifoodToBf.get(ifoodId);

            // Tentar match por externalCode ou nome exato se não houver mapeamento
            if (matchedBfId == null) {
                matchedBfId = bfProducts.stream()
                        .filter(p -> (p.externalCode() != null && p.externalCode().equals(item.item().externalCode())) ||
                                     p.name().equalsIgnoreCase(item.products().get(0).name()))
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
                BigDecimal ifoodPrice = item.item().price() != null ? item.item().price().value() : BigDecimal.ZERO;
                
                if (bfPrice.compareTo(ifoodPrice) != 0) {
                    conflictFields.add("price");
                }
                
                // Comparar Nome
                if (!bfProduct.name().equals(item.products().get(0).name())) {
                    conflictFields.add("name");
                }
                
                // Comparar Status
                if (!bfProduct.status().equals(item.item().status())) {
                    // Note: iFood only has AVAILABLE/UNAVAILABLE. PAUSED in BF means UNAVAILABLE in IFood.
                    if (!(bfProduct.status().equals("PAUSED") && item.item().status().equals("UNAVAILABLE"))) {
                        conflictFields.add("status");
                    }
                }

                if (conflictFields.isEmpty()) {
                    entries.add(new CatalogDiffEntry("MATCH", bfProduct, item.products().get(0), List.of()));
                    matches++;
                } else {
                    entries.add(new CatalogDiffEntry("CONFLICT", bfProduct, item.products().get(0), conflictFields));
                    conflicts++;
                }
            } else {
                // Apenas no iFood
                entries.add(new CatalogDiffEntry("IFOOD_ONLY", null, item.products().get(0), List.of()));
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
}
