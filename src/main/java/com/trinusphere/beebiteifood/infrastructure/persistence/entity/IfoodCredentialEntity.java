package com.trinusphere.beebiteifood.infrastructure.persistence.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "ifood_credential",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "store_id"),
                @UniqueConstraint(columnNames = "catalog_id"),
                @UniqueConstraint(columnNames = "merchant_id")
        })
public class IfoodCredentialEntity {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "store_id", nullable = false, unique = true)
    private UUID storeId;

    @Column(name = "catalog_id", nullable = false, unique = true)
    private UUID catalogId;

    @Column(name = "client_id", nullable = false)
    private String clientId;

    @Column(name = "encrypted_client_secret", nullable = false, columnDefinition = "TEXT")
    private String encryptedClientSecret;

    @Column(name = "merchant_id", nullable = false, unique = true)
    private String merchantId;

    @Column(name = "access_token", columnDefinition = "TEXT")
    private String accessToken;

    @Column(name = "refresh_token", columnDefinition = "TEXT")
    private String refreshToken;

    @Column(name = "authorization_code", length = 32)
    private String authorizationCode;

    @Column(name = "token_expires_at")
    private LocalDateTime tokenExpiresAt;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public UUID getId()                        { return id; }
    public void setId(UUID id)                 { this.id = id; }
    public UUID getStoreId()                   { return storeId; }
    public void setStoreId(UUID storeId)       { this.storeId = storeId; }
    public UUID getCatalogId()                 { return catalogId; }
    public void setCatalogId(UUID catalogId)   { this.catalogId = catalogId; }
    public String getClientId()                { return clientId; }
    public void setClientId(String clientId)   { this.clientId = clientId; }
    public String getEncryptedClientSecret()   { return encryptedClientSecret; }
    public void setEncryptedClientSecret(String s) { this.encryptedClientSecret = s; }
    public String getMerchantId()              { return merchantId; }
    public void setMerchantId(String m)        { this.merchantId = m; }
    public String getAccessToken()             { return accessToken; }
    public void setAccessToken(String t)       { this.accessToken = t; }
    public String getRefreshToken()            { return refreshToken; }
    public void setRefreshToken(String t)      { this.refreshToken = t; }
    public String getAuthorizationCode()       { return authorizationCode; }
    public void setAuthorizationCode(String a) { this.authorizationCode = a; }
    public LocalDateTime getTokenExpiresAt()   { return tokenExpiresAt; }
    public void setTokenExpiresAt(LocalDateTime t) { this.tokenExpiresAt = t; }
    public boolean isEnabled()                 { return enabled; }
    public void setEnabled(boolean enabled)    { this.enabled = enabled; }
    public LocalDateTime getCreatedAt()        { return createdAt; }
    public void setCreatedAt(LocalDateTime t)  { this.createdAt = t; }
    public LocalDateTime getUpdatedAt()        { return updatedAt; }
    public void setUpdatedAt(LocalDateTime t)  { this.updatedAt = t; }
}
