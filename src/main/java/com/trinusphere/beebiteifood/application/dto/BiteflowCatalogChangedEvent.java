package com.trinusphere.beebiteifood.application.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BiteflowCatalogChangedEvent(
        UUID catalogId,
        UUID storeId,
        UUID productId,
        String changeType,
        BigDecimal effectiveIfoodPrice,
        String globalStatus
) {
    public BiteflowCatalogChangedEvent(UUID storeId, UUID productId, String changeType) {
        this(null, storeId, productId, changeType, null, null);
    }
}
