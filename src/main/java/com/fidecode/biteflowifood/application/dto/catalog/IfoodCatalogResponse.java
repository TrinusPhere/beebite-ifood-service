package com.fidecode.biteflowifood.application.dto.catalog;

import java.math.BigDecimal;
import java.util.List;

public class IfoodCatalogResponse {

    public record IfoodCatalog(
            String catalogId,
            String context,
            String status) {
    }

    public record IfoodCategory(
            String id,
            String name,
            String status,
            String template) {
    }

    public record IfoodItemFull(
            IfoodItemData item,
            List<IfoodProduct> products,
            List<IfoodOptionGroup> optionGroups,
            List<IfoodOption> options) {
    }

    public record IfoodItemData(
            String id,
            String type,
            String categoryId,
            String status,
            IfoodPrice price,
            String externalCode,
            List<IfoodContextModifier> contextModifiers) {
    }

    public record IfoodProduct(
            String id,
            String name,
            String description,
            String imagePath) {
    }

    public record IfoodOptionGroup(
            String id,
            String name,
            String optionGroupType,
            String associationType,
            int min,
            int max,
            List<String> optionIds) {
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
