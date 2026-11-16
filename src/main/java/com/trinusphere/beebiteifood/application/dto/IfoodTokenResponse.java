package com.trinusphere.beebiteifood.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO — Resposta de autenticação OAuth do iFood.
 */
public record IfoodTokenResponse(
        @JsonProperty("accessToken") String accessToken,
        @JsonProperty("type") String type,
        @JsonProperty("expiresIn") long expiresIn,
        @JsonProperty("refreshToken") String refreshToken
) {}
