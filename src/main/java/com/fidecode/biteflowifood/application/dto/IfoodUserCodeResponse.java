package com.fidecode.biteflowifood.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO — Resposta para a requisição de User Code do iFood (OAuth Distribuído).
 */
public record IfoodUserCodeResponse(
        @JsonProperty("userCode") String userCode,
        @JsonProperty("authorizationCodeVerifier") String authorizationCodeVerifier,
        @JsonProperty("verificationUrl") String verificationUrl,
        @JsonProperty("verificationUrlComplete") String verificationUrlComplete,
        @JsonProperty("expiresIn") long expiresIn
) {}
