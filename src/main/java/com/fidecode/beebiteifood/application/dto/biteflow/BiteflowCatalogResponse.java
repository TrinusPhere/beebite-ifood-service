package com.fidecode.beebiteifood.application.dto.biteflow;

import java.math.BigDecimal;
import java.util.List;

public class BiteflowCatalogResponse {

    public record CategoryResponse(
            String id,
            String name,
            String status,
            Integer displayOrder,
            List<String> defaultModifierGroupIds) {
    }

    public record ProductResponse(
            String id,
            String name,
            String categoryId,
            String description,
            BigDecimal price,
            BigDecimal estimatedCost,
            String imageUrl,
            Integer stock,
            String status,
            String externalCode,
            String catalogItemType,
            List<ChannelPriceDto> channelPrices,
            String productType,
            Boolean trackStock,
            String productionSectorId,
            String saleUnit,
            Boolean availableDigitalMenu,
            Boolean availablePdv,
            List<TechnicalSheetItemDto> technicalSheet,
            List<String> removableItemIds,
            List<String> modifierGroupIds) {
    }

    public record ChannelPriceDto(String channel, BigDecimal price) {
    }

    public record TechnicalSheetItemDto(String ingredientId, BigDecimal quantity) {
    }

    public record ModifierGroupResponse(
            String id,
            String name,
            String type,
            Boolean required,
            Integer minSelections,
            Integer maxSelections,
            String icon,
            String semanticType,
            List<ModifierOptionDto> options) {
    }

    public record ModifierOptionDto(
            String id,
            String name,
            BigDecimal price,
            String linkedProductId) {
    }
}
