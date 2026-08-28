package com.fidecode.biteflowifood.domain.credential.entity;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Aggregate Root — IfoodCredential.
 * POJO puro, zero dependência de framework.
 * Armazena as credenciais OAuth do iFood por Store.
 */
public class IfoodCredential {

    private UUID id;
    private final UUID storeId;
    private String clientId;
    private String encryptedClientSecret;
    private String merchantId;
    private String accessToken;
    private String refreshToken;
    private String authorizationCode;
    private LocalDateTime tokenExpiresAt;
    private boolean enabled;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** Token é considerado inválido se expirar em menos de 5 minutos */
    private static final int TOKEN_VALIDITY_MARGIN_MINUTES = 5;

    // ── Construtor principal ───────────────────────────────────────────────────

    public IfoodCredential(UUID storeId, String clientId, String encryptedClientSecret, String merchantId) {
        this.storeId = storeId;
        this.clientId = clientId;
        this.encryptedClientSecret = encryptedClientSecret;
        this.merchantId = merchantId;
        this.enabled = true;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // ── Construtor privado para reconstitute ──────────────────────────────────

    private IfoodCredential(UUID id, UUID storeId, String clientId, String encryptedClientSecret,
                             String merchantId, String accessToken, String refreshToken, String authorizationCode,
                             LocalDateTime tokenExpiresAt, boolean enabled, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.storeId = storeId;
        this.clientId = clientId;
        this.encryptedClientSecret = encryptedClientSecret;
        this.merchantId = merchantId;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.authorizationCode = authorizationCode;
        this.tokenExpiresAt = tokenExpiresAt;
        this.enabled = enabled;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // ── Factory para reconstrução do banco (P5 — Encapsulamento) ─────────────

    public static IfoodCredential reconstitute(
            UUID id, UUID storeId, String clientId, String encryptedClientSecret,
            String merchantId, String accessToken, String refreshToken, String authorizationCode,
            LocalDateTime tokenExpiresAt, boolean enabled, LocalDateTime createdAt, LocalDateTime updatedAt) {

        return new IfoodCredential(id, storeId, clientId, encryptedClientSecret,
                merchantId, accessToken, refreshToken, authorizationCode, tokenExpiresAt, enabled, createdAt, updatedAt);
    }

    // ── Comportamentos ────────────────────────────────────────────────────────

    /**
     * Retorna true se o access token é válido e não expira nos próximos 5 minutos.
     */
    public boolean isTokenValid() {
        if (accessToken == null || tokenExpiresAt == null) return false;
        return tokenExpiresAt.isAfter(LocalDateTime.now().plusMinutes(TOKEN_VALIDITY_MARGIN_MINUTES));
    }

    /**
     * Atualiza o access token e calcula a data de expiração.
     *
     * @param newAccessToken  token obtido do OAuth do iFood
     * @param newRefreshToken refresh token obtido do OAuth do iFood
     * @param expiresInSeconds duração do token em segundos
     */
    public void updateToken(String newAccessToken, String newRefreshToken, long expiresInSeconds) {
        this.accessToken = newAccessToken;
        if (newRefreshToken != null && !newRefreshToken.isBlank()) {
            this.refreshToken = newRefreshToken;
        }
        this.tokenExpiresAt = LocalDateTime.now().plusSeconds(expiresInSeconds);
        this.updatedAt = LocalDateTime.now();
    }

    public void setAuthorizationCode(String authorizationCode) {
        this.authorizationCode = authorizationCode;
        this.updatedAt = LocalDateTime.now();
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public UUID getId()                        { return id; }
    public UUID getStoreId()                   { return storeId; }
    public String getClientId()               { return clientId; }
    public String getEncryptedClientSecret()  { return encryptedClientSecret; }
    public String getMerchantId()             { return merchantId; }
    public String getAccessToken()            { return accessToken; }
    public String getRefreshToken()           { return refreshToken; }
    public String getAuthorizationCode()      { return authorizationCode; }
    public LocalDateTime getTokenExpiresAt()  { return tokenExpiresAt; }
    public boolean isEnabled()                { return enabled; }
    public LocalDateTime getCreatedAt()       { return createdAt; }
    public LocalDateTime getUpdatedAt()       { return updatedAt; }

    // ── Setters mínimos (apenas para persistência JPA) ───────────────────────

    public void setId(UUID id)                          { this.id = id; }
    public void setEnabled(boolean enabled)             { this.enabled = enabled;             this.updatedAt = LocalDateTime.now(); }
    public void setClientId(String clientId)            { this.clientId = clientId;           this.updatedAt = LocalDateTime.now(); }
    public void setMerchantId(String merchantId)        { this.merchantId = merchantId;       this.updatedAt = LocalDateTime.now(); }
    public void setEncryptedClientSecret(String secret) { this.encryptedClientSecret = secret; this.updatedAt = LocalDateTime.now(); }
}
