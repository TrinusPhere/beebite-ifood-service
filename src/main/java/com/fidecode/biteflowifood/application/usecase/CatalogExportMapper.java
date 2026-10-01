package com.fidecode.biteflowifood.application.usecase;

import com.fidecode.biteflowifood.application.dto.biteflow.BiteflowCatalogResponse.*;
import com.fidecode.biteflowifood.application.dto.catalog.IfoodCatalogRequest.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class CatalogExportMapper {

    private CatalogExportMapper() {}

    public static IfoodCreateCategoryRequest toIfoodCategory(CategoryResponse category) {
        return new IfoodCreateCategoryRequest(
                category.name(),
                category.status(),
                "DEFAULT"
        );
    }

    /**
     * Converte um produto BiteFlow para o formato de criação do iFood Catalog v2.0.
     * PUT /catalog/v2.0/merchants/{merchantId}/items
     *
     * @param product        produto do BiteFlow
     * @param productGroups  modifier groups vinculados ao produto
     * @param ifoodCategoryId ID da categoria no iFood (já criada)
     */
    public static IfoodCreateProductRequest toIfoodProduct(ProductResponse product,
                                                            List<ModifierGroupResponse> productGroups,
                                                            String ifoodCategoryId,
                                                            String generatedItemId) {
        List<IfoodOptionGroup> optionGroups = new ArrayList<>();
        List<IfoodOption> options = new ArrayList<>();
        List<IfoodProductData> products = new ArrayList<>();

        List<IfoodOptionGroupReference> mainOptionGroupIds = new ArrayList<>();

        if (productGroups != null && !productGroups.isEmpty()) {
            for (ModifierGroupResponse g : productGroups) {
                int min = g.minSelections() != null ? g.minSelections() : (Boolean.TRUE.equals(g.required()) ? 1 : 0);
                int max = g.maxSelections() != null ? g.maxSelections() : 1;
                if (min > max) {
                    max = min;
                }

                String groupId = UUID.randomUUID().toString();
                mainOptionGroupIds.add(new IfoodOptionGroupReference(groupId, min, max));
                List<String> optionIds = new ArrayList<>();

                if (g.options() != null) {
                    for (ModifierOptionDto opt : g.options()) {
                        String optProductId = opt.linkedProductId() != null ? opt.linkedProductId() : UUID.randomUUID().toString();
                        String optionId = UUID.randomUUID().toString();
                        optionIds.add(optionId);

                        options.add(new IfoodOption(
                                optionId,
                                "AVAILABLE",
                                optProductId,
                                new IfoodPrice(opt.price() != null ? opt.price() : BigDecimal.ZERO, opt.price() != null ? opt.price() : BigDecimal.ZERO),
                                null
                        ));

                        // Adiciona o produto vinculado à lista de produtos da requisição
                        products.add(new IfoodProductData(
                                optProductId,
                                opt.name(),
                                null,
                                null,
                                new ArrayList<>() // Produtos de opção geralmente não têm sub-opções nesta integração
                        ));
                    }
                }

                optionGroups.add(new IfoodOptionGroup(
                        groupId,
                        g.name(),
                        mapModifierSemanticType(g.semanticType()),
                        "AVAILABLE",
                        min,
                        max,
                        optionIds
                ));
            }
        }

        // O produto principal também precisa estar na lista de products, linkando os grupos de opções
        products.add(new IfoodProductData(
                product.id(),
                product.name(),
                product.description(),
                product.externalCode(),
                mainOptionGroupIds
        ));

        BigDecimal price = BigDecimal.ZERO;
        if (product.channelPrices() != null) {
            price = product.channelPrices().stream()
                    .filter(c -> "IFOOD".equals(c.channel()))
                    .map(ChannelPriceDto::price)
                    .findFirst()
                    .orElse(product.price() != null ? product.price() : BigDecimal.ZERO);
        } else if (product.price() != null) {
            price = product.price();
        }

        // iFood v2.0: item precisa de name, description, serves, productId
        IfoodItemData itemData = new IfoodItemData(
                generatedItemId, // Use generated ID instead of Biteflow ID to prevent duplicate ID issues with product ID
                product.id(), // productId = referência ao produto principal na lista products
                product.name(),
                product.description(),
                mapProductType(product.catalogItemType()),
                ifoodCategoryId,
                product.status() != null ? product.status() : "AVAILABLE",
                new IfoodPrice(price, price),
                product.externalCode(),
                "NOT_APPLICABLE" // serves: padrão para lanches
        );

        return new IfoodCreateProductRequest(
                itemData,
                products,
                optionGroups,
                options
        );
    }

    private static String mapModifierSemanticType(String semanticType) {
        if (semanticType == null) return "OFFER_UNIT";
        return switch (semanticType) {
            case "SPECIFICATION" -> "SPECIFICATION";
            case "INGREDIENTS" -> "INGREDIENTS";
            case "CUTLERY" -> "CUTLERY";
            default -> "OFFER_UNIT";
        };
    }

    private static String mapProductType(String catalogItemType) {
        if (catalogItemType == null) return "DEFAULT";
        return switch (catalogItemType) {
            case "PIZZA" -> "PIZZA";
            case "COMBO" -> "COMBO_V2";
            default -> "DEFAULT";
        };
    }
}
