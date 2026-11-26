package com.trinusphere.beebiteifood.application.dto.biteflow;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public class BiteflowCatalogResponse {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record StoreCatalogContextResponse(
            UUID storeId,
            UUID tenantId,
            UUID catalogId,
            String catalogName,
            String syncMode,
            boolean active) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CategoryResponse(
            String id,
            String name,
            String status,
            Integer displayOrder,
            List<String> defaultModifierGroupIds) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
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
            String globalStatus,
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

        public ProductResponse(
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
            this(
                    id, name, categoryId, description, price, estimatedCost, imageUrl, stock,
                    status, status, externalCode, catalogItemType, channelPrices, productType,
                    trackStock, productionSectorId, saleUnit, availableDigitalMenu, availablePdv,
                    technicalSheet, removableItemIds, modifierGroupIds
            );
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ChannelPriceDto(String channel, BigDecimal price) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TechnicalSheetItemDto(String ingredientId, BigDecimal quantity) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
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

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ModifierOptionDto(
            String id,
            String name,
            BigDecimal price,
            String linkedProductId) {
    }
}
