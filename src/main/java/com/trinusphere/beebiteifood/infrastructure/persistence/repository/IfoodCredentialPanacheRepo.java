package com.trinusphere.beebiteifood.infrastructure.persistence.repository;

import com.trinusphere.beebiteifood.infrastructure.persistence.entity.IfoodCredentialEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;
import java.util.UUID;

/** Repositório Panache auxiliar — não implementa port de domínio (evita conflito de assinaturas). */
@ApplicationScoped
public class IfoodCredentialPanacheRepo implements PanacheRepositoryBase<IfoodCredentialEntity, UUID> {

    public Optional<IfoodCredentialEntity> findByStoreId(UUID storeId) {
        return find("storeId", storeId).firstResultOptional();
    }

    public void deleteByStoreId(UUID storeId) {
        delete("storeId", storeId);
    }
}
