package com.fidecode.biteflowifood.application.dto.catalog;

import com.fasterxml.jackson.annotation.JsonInclude;

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
            String serves,
            @JsonInclude(JsonInclude.Include.NON_EMPTY)
            List<IfoodContextModifier> contextModifiers,
            @JsonInclude(JsonInclude.Include.NON_EMPTY)
            List<IfoodShift> shifts) {

        public IfoodItemData(String id, String productId, String name, String description,
                             String type, String categoryId, String status,
                             IfoodPrice price, String externalCode, String serves) {
            this(id, productId, name, description, type, categoryId, status, price, externalCode, serves, null, null);
        }
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

    /**
     * DTO para atualização de preço via Merge Patch no endpoint PATCH /items/{itemId}.
     * A OpenAPI v2.0 exige objeto "price": {"value": ..., "originalValue": ...}.
     */
    public record IfoodUpdatePriceRequest(
            IfoodPrice price) {

        public IfoodUpdatePriceRequest(BigDecimal value) {
            this(new IfoodPrice(value, value));
        }

        public IfoodUpdatePriceRequest(BigDecimal value, BigDecimal originalValue) {
            this(new IfoodPrice(value, originalValue));
        }

        public BigDecimal value() {
            return price != null ? price.value() : null;
        }
    }

    /**
     * DTO para atualização de status via Merge Patch no endpoint PATCH /items/{itemId}.
     */
    public record IfoodUpdateStatusRequest(
            String status) {
    }

    /**
     * DTO completo para Merge Patch no endpoint PATCH /items/{itemId} (RFC 7386).
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record IfoodItemPatchRequest(
            String status,
            IfoodPrice price,
            String externalCode,
            Integer index,
            List<IfoodShift> shifts) {
    }

    public record IfoodShift(
            String startTime,
            String endTime,
            Boolean monday,
            Boolean tuesday,
            Boolean wednesday,
            Boolean thursday,
            Boolean friday,
            Boolean saturday,
            Boolean sunday) {
    }

    public record IfoodContextModifier(
            String catalogContext,
            IfoodPrice price,
            String status,
            String externalCode) {
    }

    public record IfoodUpdateProductRequest(
            String name,
            String description,
            String externalCode) {
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

    // --- Endpoints de lote (Batch) ---

    public record IfoodBatchPriceItem(
            String productId,
            String externalCode,
            IfoodPrice price,
            List<String> resources,
            String catalogId) {
    }

    public record IfoodBatchStatusItem(
            String productId,
            String externalCode,
            String status,
            List<String> resources,
            String catalogId) {
    }

    public record IfoodBatchResponse(
            String batchId,
            String url) {
    }

    public record IfoodBatchStatusResponse(
            String batchId,
            String status,
            List<IfoodBatchResultItem> results) {
    }

    public record IfoodBatchResultItem(
            String resource,
            String result,
            List<String> errors) {
    }

    // --- Endpoints de opções / complementos ---

    public record IfoodUpdateOptionPriceRequest(
            String optionId,
            IfoodPrice price,
            String parentCustomizationOptionId,
            List<IfoodPriceByCatalog> priceByCatalog) {
    }

    public record IfoodPriceByCatalog(
            BigDecimal value,
            BigDecimal originalValue,
            String catalogContext) {
    }

    public record IfoodUpdateOptionStatusRequest(
            String optionId,
            String status,
            String parentCustomizationOptionId,
            List<IfoodStatusByCatalog> statusByCatalog) {
    }

    public record IfoodStatusByCatalog(
            String status,
            String catalogContext) {
    }

    public record IfoodUpdateOptionGroupStatusRequest(
            String status) {
    }
}
