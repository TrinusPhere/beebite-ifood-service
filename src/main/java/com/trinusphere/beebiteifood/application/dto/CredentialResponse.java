package com.trinusphere.beebiteifood.application.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record CredentialResponse(
        UUID id,
        UUID storeId,
        UUID catalogId,
        String clientId,
        String merchantId,
        boolean enabled,
        boolean tokenValid,
        LocalDateTime updatedAt
) {
    public CredentialResponse(
            UUID id,
            UUID storeId,
            String clientId,
            String merchantId,
            boolean enabled,
            boolean tokenValid,
            LocalDateTime updatedAt) {
        this(id, storeId, storeId, clientId, merchantId, enabled, tokenValid, updatedAt);
    }
}
