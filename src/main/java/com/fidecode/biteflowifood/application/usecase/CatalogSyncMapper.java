package com.fidecode.biteflowifood.application.usecase;

import com.fidecode.biteflowifood.application.dto.biteflow.BiteflowCatalogRequest.*;
import com.fidecode.biteflowifood.application.dto.catalog.IfoodCatalogResponse.*;

import java.util.ArrayList;
import java.util.List;

public final class CatalogSyncMapper {

    private CatalogSyncMapper() {
    }

    public static CreateCategoryRequest toCreateCategoryRequest(IfoodCategory category) {
        return new CreateCategoryRequest(category.name(), category.status(), 0, null);
    }

    public static CreateProductRequest toCreateProductRequest(IfoodItemFull fullItem, String context) {
        IfoodItemData data = fullItem.item();
        IfoodProduct prod = fullItem.products() != null && !fullItem.products().isEmpty() ? fullItem.products().get(0) : null;
        
        String name = prod != null ? prod.name() : "Produto sem nome";
        String description = prod != null ? prod.description() : null;
        String imageUrl = prod != null ? prod.imagePath() : null;

        List<ChannelPriceDto> channelPrices = new ArrayList<>();
        if (data.price() != null && data.price().value() != null) {
            channelPrices.add(new ChannelPriceDto("IFOOD", data.price().value()));
        }

        List<String> modifierGroupIds = new ArrayList<>();
        // In the real flow, modifierGroupIds would be resolved after creating groups in BiteFlow.
        // For the DTO mapping phase, we just map basic fields.
        
        return new CreateProductRequest(
                name,
                null, // categoryId should be resolved externally
                description,
                null, // base price in PDV, maybe we can use iFood price as fallback when creating
                imageUrl,
                null,
                data.status(),
                data.externalCode(),
                mapProductType(data.type()),
                channelPrices,
                "RESALE", // Defaulting to RESALE as we don't know
                false,
                null,
                "UN",
                true,
                true,
                null,
                null,
                modifierGroupIds
        );
    }

    public static CreateModifierGroupRequest toCreateModifierGroupRequest(IfoodOptionGroup group, IfoodItemFull fullItem) {
        boolean required = group.min() > 0;
        String type = (group.min() == 1 && group.max() == 1) ? "RADIO" : "CHECKBOX";
        String semanticType = mapModifierType(group.optionGroupType());

        List<ModifierOptionDto> options = new ArrayList<>();
        if (group.optionIds() != null && group.optionIds().isArray() && fullItem.options() != null) {
            for (com.fasterxml.jackson.databind.JsonNode optNode : group.optionIds()) {
                String optId = optNode.asText();
                IfoodOption ifoodOpt = fullItem.options().stream()
                        .filter(o -> o.id().equals(optId))
                        .findFirst().orElse(null);

                if (ifoodOpt != null) {
                    // Resolve product name from the products list
                    String optName = "Opção";
                    if (fullItem.products() != null) {
                        IfoodProduct optProd = fullItem.products().stream()
                                .filter(p -> p.id().equals(ifoodOpt.productId()))
                                .findFirst().orElse(null);
                        if (optProd != null) {
                            optName = optProd.name();
                        }
                    }

                    options.add(new ModifierOptionDto(
                            optName,
                            ifoodOpt.price() != null ? ifoodOpt.price().value() : null,
                            null // linkedProductId would be resolved later if it's a COMBO
                    ));
                }
            }
        }

        return new CreateModifierGroupRequest(
                group.name(),
                type,
                required,
                group.min(),
                group.max(),
                null,
                semanticType,
                options
        );
    }

    public static String mapModifierType(String optionGroupType) {
        if (optionGroupType == null) return "OFFER_UNIT";
        return switch (optionGroupType) {
            case "MIN_MAX" -> "SPECIFICATION";
            case "ADDITIONAL" -> "OFFER_UNIT";
            case "CUTLERY" -> "CUTLERY";
            default -> "OFFER_UNIT";
        };
    }

    public static String mapProductType(String ifoodType) {
        if (ifoodType == null) return "DEFAULT";
        return switch (ifoodType) {
            case "PIZZA" -> "PIZZA";
            case "COMBO_V2" -> "COMBO";
            default -> "DEFAULT";
        };
    }
}
