package com.trinusphere.beebiteifood.infrastructure.persistence.mapper;

import com.trinusphere.beebiteifood.domain.credential.entity.IfoodCredential;
import com.trinusphere.beebiteifood.infrastructure.persistence.entity.IfoodCredentialEntity;

import java.time.LocalDateTime;

public final class IfoodCredentialMapper {

    private IfoodCredentialMapper() {}

    public static IfoodCredential toDomain(IfoodCredentialEntity entity) {
        return IfoodCredential.reconstitute(
                entity.getId(),
                entity.getStoreId(),
                entity.getCatalogId() != null ? entity.getCatalogId() : entity.getStoreId(),
                entity.getClientId(),
                entity.getEncryptedClientSecret(),
                entity.getMerchantId(),
                entity.getAccessToken(),
                entity.getRefreshToken(),
                entity.getAuthorizationCode(),
                entity.getTokenExpiresAt(),
                entity.isEnabled(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public static IfoodCredentialEntity toEntity(IfoodCredential domain) {
        IfoodCredentialEntity entity = new IfoodCredentialEntity();
        entity.setId(domain.getId());
        entity.setStoreId(domain.getStoreId());
        entity.setCatalogId(domain.getCatalogId() != null ? domain.getCatalogId() : domain.getStoreId());
        entity.setClientId(domain.getClientId());
        entity.setEncryptedClientSecret(domain.getEncryptedClientSecret());
        entity.setMerchantId(domain.getMerchantId());
        entity.setAccessToken(domain.getAccessToken());
        entity.setRefreshToken(domain.getRefreshToken());
        entity.setAuthorizationCode(domain.getAuthorizationCode());
        entity.setTokenExpiresAt(domain.getTokenExpiresAt());
        entity.setEnabled(domain.isEnabled());
        entity.setCreatedAt(domain.getCreatedAt() != null ? domain.getCreatedAt() : LocalDateTime.now());
        entity.setUpdatedAt(domain.getUpdatedAt() != null ? domain.getUpdatedAt() : LocalDateTime.now());
        return entity;
    }
}
