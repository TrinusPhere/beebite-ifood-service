package com.fidecode.biteflowifood.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * DTO — Requisição para salvar/atualizar credenciais iFood de uma Store.
 */
public record SaveCredentialRequest(
        @NotNull UUID storeId,
        @NotBlank String clientId,
        @NotBlank String clientSecret,
        @NotBlank String merchantId
) {}
