package com.trinusphere.beebiteifood.domain.credential.repository;

import com.trinusphere.beebiteifood.domain.credential.entity.IfoodCredential;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Port (Interface) — IfoodCredentialRepository.
 * Define o contrato de persistência de credenciais iFood.
 * Implementado na camada de infraestrutura (Panache).
 */
public interface IfoodCredentialRepository {

    IfoodCredential save(IfoodCredential credential);

    Optional<IfoodCredential> findByStoreId(UUID storeId);

    /** Retorna todas as credenciais habilitadas para o ciclo de polling. */
    List<IfoodCredential> findAllEnabled();

    void deleteByStoreId(UUID storeId);
}
