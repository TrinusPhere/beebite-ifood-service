package com.fidecode.biteflowifood.application.dto.catalog;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.annotation.JsonAlias;
import java.math.BigDecimal;
import java.util.List;

public class IfoodCatalogResponse {

    public record IfoodCatalog(
            String catalogId,
            List<String> context,
            String status) {
    }

    public record IfoodCategory(
            String id,
            String name,
            String status,
            String template) {
    }

    public record IfoodCategoryItemsResponse(
            String categoryId,
            List<IfoodItemData> items,
            List<IfoodProduct> products,
            List<IfoodOptionGroup> optionGroups,
            List<IfoodOption> options) {
    }

    public record IfoodItemFull(
            IfoodItemData item,
            List<IfoodProduct> products,
            List<IfoodOptionGroup> optionGroups,
            List<IfoodOption> options) {
    }

    public record IfoodItemData(
            @JsonAlias({"id", "itemId"})
            String id,
            String type,
            String categoryId,
            String status,
            IfoodPrice price,
            String externalCode,
            String productId,
            JsonNode contextModifiers) {
    }

    public record IfoodProduct(
            String id,
            String name,
            String description,
            String imagePath,
            JsonNode optionGroups) {
    }

    public record IfoodOptionGroup(
            String id,
            String name,
            String optionGroupType,
            String associationType,
            int min,
            int max,
            JsonNode optionIds) {
    }

    public record IfoodOption(
            String id,
            String productId,
            IfoodPrice price,
            String status,
            String externalCode) {
    }

    public record IfoodPrice(
            BigDecimal value,
            BigDecimal originalValue) {
    }

    public record IfoodContextModifier(
            String catalogContext,
            IfoodPrice price,
            String status,
            String externalCode) {
    }
}
