package com.fidecode.biteflowifood.infrastructure.persistence.mapper;

import com.fidecode.biteflowifood.domain.credential.entity.IfoodCredential;
import com.fidecode.biteflowifood.infrastructure.persistence.entity.IfoodCredentialEntity;

import java.time.LocalDateTime;

/** Mapper entre IfoodCredential (domínio) e IfoodCredentialEntity (JPA). */
public final class IfoodCredentialMapper {

    private IfoodCredentialMapper() {}

    public static IfoodCredential toDomain(IfoodCredentialEntity entity) {
        return IfoodCredential.reconstitute(
                entity.getId(),
                entity.getStoreId(),
                entity.getClientId(),
                entity.getEncryptedClientSecret(),
                entity.getMerchantId(),
                entity.getAccessToken(),
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
        entity.setClientId(domain.getClientId());
        entity.setEncryptedClientSecret(domain.getEncryptedClientSecret());
        entity.setMerchantId(domain.getMerchantId());
        entity.setAccessToken(domain.getAccessToken());
        entity.setTokenExpiresAt(domain.getTokenExpiresAt());
        entity.setEnabled(domain.isEnabled());
        entity.setCreatedAt(domain.getCreatedAt() != null ? domain.getCreatedAt() : LocalDateTime.now());
        entity.setUpdatedAt(domain.getUpdatedAt() != null ? domain.getUpdatedAt() : LocalDateTime.now());
        return entity;
    }
}
