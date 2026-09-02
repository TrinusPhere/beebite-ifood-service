package com.fidecode.biteflowifood.application.dto.catalog;

import java.math.BigDecimal;
import java.util.List;

public class IfoodCatalogRequest {

    public record IfoodCreateCategoryRequest(
            String name,
            String status,
            String template) {
    }

    public record IfoodCreateItemRequest(
            String type,
            String categoryId,
            String name,
            String description,
            String externalCode,
            String status,
            IfoodPrice price,
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
            String name,
            String optionGroupType,
            String associationType,
            int min,
            int max,
            List<String> optionIds) {
    }

    public record IfoodOption(
            String id,
            String externalCode,
            IfoodPrice price,
            String status,
            String name) {
    }
}
