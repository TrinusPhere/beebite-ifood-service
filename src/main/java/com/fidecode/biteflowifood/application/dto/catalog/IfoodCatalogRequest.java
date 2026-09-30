package com.fidecode.biteflowifood.application.dto.catalog;

import java.math.BigDecimal;
import java.util.List;

public class IfoodCatalogRequest {

    public record IfoodCreateCategoryRequest(
            String name,
            String status,
            String template) {
    }

    /**
     * Dados do item principal no iFood Catalog v2.0.
     * Usado no PUT /merchants/{merchantId}/items
     */
    public record IfoodItemData(
            String id,
            String productId,
            String name,
            String description,
            String type,
            String categoryId,
            String status,
            IfoodPrice price,
            String externalCode,
            String serves) {
    }

    public record IfoodProductData(
            String id,
            String name,
            String description,
            String externalCode,
            List<IfoodOptionGroupReference> optionGroups) {
    }

    public record IfoodOptionGroupReference(
            String id,
            int min,
            int max) {
    }

    /**
     * Request para criação/atualização de item no iFood Catalog v2.0.
     * PUT /catalog/v2.0/merchants/{merchantId}/items
     */
    public record IfoodCreateProductRequest(
            IfoodItemData item,
            List<IfoodProductData> products,
            List<IfoodOptionGroup> optionGroups,
            List<IfoodOption> options) {
    }

    public record IfoodUpdatePriceRequest(
            BigDecimal value) {
    }

    public record IfoodUpdateStatusRequest(
            String status) {
    }

    public record IfoodPrice(
            BigDecimal value,
            BigDecimal originalValue) {
    }

    public record IfoodOptionGroup(
            String id,
            String name,
            String optionGroupType,
            String status,
            int min,
            int max,
            List<String> optionIds) {
    }

    public record IfoodOption(
            String id,
            String status,
            String productId,
            IfoodPrice price,
            String externalCode) {
    }
}
