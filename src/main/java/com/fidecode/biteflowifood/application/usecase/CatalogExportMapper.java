package com.fidecode.biteflowifood.application.usecase;

import com.fidecode.biteflowifood.application.dto.biteflow.BiteflowCatalogResponse.*;
import com.fidecode.biteflowifood.application.dto.catalog.IfoodCatalogRequest.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

public final class CatalogExportMapper {

    private CatalogExportMapper() {}

    public static IfoodCreateCategoryRequest toIfoodCategory(CategoryResponse category) {
        return new IfoodCreateCategoryRequest(
                category.name(),
                category.status(),
                "DEFAULT"
        );
    }

    public static IfoodCreateItemRequest toIfoodItem(ProductResponse product, List<ModifierGroupResponse> productGroups) {
        // Map modifier groups
        List<IfoodOptionGroup> optionGroups = null;
        List<IfoodOption> options = null;

        if (productGroups != null && !productGroups.isEmpty()) {
            optionGroups = productGroups.stream().map(g -> new IfoodOptionGroup(
                    g.name(),
                    mapModifierSemanticType(g.semanticType()),
                    "ADDITIONAL", // Default for iFood when not specified
                    g.minSelections(),
                    g.maxSelections(),
                    g.options().stream().map(ModifierOptionDto::id).collect(Collectors.toList())
            )).collect(Collectors.toList());

            options = productGroups.stream()
                    .flatMap(g -> g.options().stream())
                    .map(opt -> new IfoodOption(
                            opt.id(),
                            null, // externalCode
                            new IfoodPrice(opt.price(), opt.price()),
                            "AVAILABLE",
                            opt.name()
                    )).collect(Collectors.toList());
        }

        BigDecimal price = BigDecimal.ZERO;
        if (product.channelPrices() != null) {
            price = product.channelPrices().stream()
                    .filter(c -> "IFOOD".equals(c.channel()))
                    .map(ChannelPriceDto::price)
                    .findFirst()
                    .orElse(product.price());
        } else {
            price = product.price();
        }

        return new IfoodCreateItemRequest(
                mapProductType(product.catalogItemType()),
                null, // categoryId should be assigned when adding to category
                product.name(),
                product.description(),
                product.externalCode(),
                product.status(),
                new IfoodPrice(price, price),
                optionGroups,
                options
        );
    }

    private static String mapModifierSemanticType(String semanticType) {
        if (semanticType == null) return "ADDITIONAL";
        return switch (semanticType) {
            case "SPECIFICATION" -> "SPECIFICATION";
            case "OFFER_UNIT" -> "ADDITIONAL";
            case "CUTLERY" -> "CUTLERY";
            default -> "ADDITIONAL";
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
