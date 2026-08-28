package com.fidecode.biteflowifood.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ExchangeAuthCodeRequest(
        @NotNull UUID storeId,
        @NotBlank String clientId,
        @NotBlank String clientSecret,
        @NotBlank String merchantId,
        @NotBlank String authorizationCode,
        @NotBlank String authorizationCodeVerifier
) {}
