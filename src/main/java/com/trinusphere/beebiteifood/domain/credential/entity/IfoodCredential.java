package com.trinusphere.beebiteifood.domain.credential.entity;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Aggregate Root — IfoodCredential.
 * POJO puro, zero dependência de framework.
 * Armazena as credenciais OAuth do iFood vinculadas à Store Delivery (CLOUD)
 * e ao Catalog compartilhado da marca.
 */
public class IfoodCredential {

    private UUID id;
    private final UUID storeId;
    private UUID catalogId;
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

    private static final int TOKEN_VALIDITY_MARGIN_MINUTES = 5;
    public static final long MAX_TOKEN_VALIDITY_SECONDS = 6L * 60 * 60;

    public IfoodCredential(UUID storeId, String clientId, String encryptedClientSecret, String merchantId) {
        this(storeId, storeId, clientId, encryptedClientSecret, merchantId);
    }

    public IfoodCredential(UUID storeId, UUID catalogId, String clientId, String encryptedClientSecret, String merchantId) {
        this.storeId = storeId;
        this.catalogId = catalogId != null ? catalogId : storeId;
        this.clientId = clientId;
        this.encryptedClientSecret = encryptedClientSecret;
        this.merchantId = merchantId;
        this.enabled = true;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    private IfoodCredential(UUID id, UUID storeId, UUID catalogId, String clientId, String encryptedClientSecret,
                             String merchantId, String accessToken, String refreshToken, String authorizationCode,
                             LocalDateTime tokenExpiresAt, boolean enabled, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.storeId = storeId;
        this.catalogId = catalogId != null ? catalogId : storeId;
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

    public static IfoodCredential reconstitute(
            UUID id, UUID storeId, String clientId, String encryptedClientSecret,
            String merchantId, String accessToken, String refreshToken, String authorizationCode,
            LocalDateTime tokenExpiresAt, boolean enabled, LocalDateTime createdAt, LocalDateTime updatedAt) {
        return reconstitute(id, storeId, storeId, clientId, encryptedClientSecret,
                merchantId, accessToken, refreshToken, authorizationCode, tokenExpiresAt, enabled, createdAt, updatedAt);
    }

    public static IfoodCredential reconstitute(
            UUID id, UUID storeId, UUID catalogId, String clientId, String encryptedClientSecret,
            String merchantId, String accessToken, String refreshToken, String authorizationCode,
            LocalDateTime tokenExpiresAt, boolean enabled, LocalDateTime createdAt, LocalDateTime updatedAt) {
        return new IfoodCredential(id, storeId, catalogId, clientId, encryptedClientSecret,
                merchantId, accessToken, refreshToken, authorizationCode, tokenExpiresAt, enabled, createdAt, updatedAt);
    }

    public boolean isTokenValid() {
        if (accessToken == null || tokenExpiresAt == null) return false;
        LocalDateTime now = LocalDateTime.now();
        if (tokenExpiresAt.isAfter(now.plusSeconds(MAX_TOKEN_VALIDITY_SECONDS))) return false;
        return tokenExpiresAt.isAfter(now.plusMinutes(TOKEN_VALIDITY_MARGIN_MINUTES));
    }

    public void updateToken(String newAccessToken, String newRefreshToken, long expiresInSeconds) {
        this.accessToken = newAccessToken;
        if (newRefreshToken != null && !newRefreshToken.isBlank()) {
            this.refreshToken = newRefreshToken;
        }
        long effectiveSeconds = Math.max(0, Math.min(expiresInSeconds, MAX_TOKEN_VALIDITY_SECONDS));
        this.tokenExpiresAt = LocalDateTime.now().plusSeconds(effectiveSeconds);
        this.updatedAt = LocalDateTime.now();
    }

    public void invalidateToken() {
        this.tokenExpiresAt = null;
        this.updatedAt = LocalDateTime.now();
    }

    public void assignCatalog(UUID catalogId) {
        if (catalogId != null) {
            this.catalogId = catalogId;
            this.updatedAt = LocalDateTime.now();
        }
    }

    public void setAuthorizationCode(String authorizationCode) {
        this.authorizationCode = authorizationCode;
        this.updatedAt = LocalDateTime.now();
    }

    public UUID getId()                        { return id; }
    public UUID getStoreId()                   { return storeId; }
    public UUID getCatalogId()                 { return catalogId; }
    public String getClientId()                { return clientId; }
    public String getEncryptedClientSecret()   { return encryptedClientSecret; }
    public String getMerchantId()              { return merchantId; }
    public String getAccessToken()             { return accessToken; }
    public String getRefreshToken()            { return refreshToken; }
    public String getAuthorizationCode()       { return authorizationCode; }
    public LocalDateTime getTokenExpiresAt()   { return tokenExpiresAt; }
    public boolean isEnabled()                 { return enabled; }
    public LocalDateTime getCreatedAt()        { return createdAt; }
    public LocalDateTime getUpdatedAt()        { return updatedAt; }

    public void setId(UUID id)                          { this.id = id; }
    public void setEnabled(boolean enabled)             { this.enabled = enabled;             this.updatedAt = LocalDateTime.now(); }
    public void setClientId(String clientId)            { this.clientId = clientId;           this.updatedAt = LocalDateTime.now(); }
    public void setMerchantId(String merchantId)        { this.merchantId = merchantId;       this.updatedAt = LocalDateTime.now(); }
    public void setEncryptedClientSecret(String secret) { this.encryptedClientSecret = secret; this.updatedAt = LocalDateTime.now(); }
}
