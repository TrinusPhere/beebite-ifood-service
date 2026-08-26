package com.fidecode.biteflowifood.application.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO — Resposta com dados das credenciais iFood (sem expor clientSecret).
 */
public record CredentialResponse(
        UUID id,
        UUID storeId,
        String clientId,
        String merchantId,
        boolean enabled,
        boolean tokenValid,
        LocalDateTime updatedAt
) {}
