package com.fidecode.biteflowifood.infrastructure.persistence.repository;

import com.fidecode.biteflowifood.domain.credential.entity.IfoodCredential;
import com.fidecode.biteflowifood.domain.credential.repository.IfoodCredentialRepository;
import com.fidecode.biteflowifood.infrastructure.persistence.entity.IfoodCredentialEntity;
import com.fidecode.biteflowifood.infrastructure.persistence.mapper.IfoodCredentialMapper;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementa o port de domínio IfoodCredentialRepository.
 * Injeta o helper Panache e converte entre domain e entity.
 * @Transactional apenas nos métodos de escrita.
 */
@ApplicationScoped
public class PanacheIfoodCredentialRepository implements IfoodCredentialRepository {

    private final IfoodCredentialPanacheRepo panacheRepo;

    @Inject
    public PanacheIfoodCredentialRepository(IfoodCredentialPanacheRepo panacheRepo) {
        this.panacheRepo = panacheRepo;
    }

    @Override
    @Transactional
    public IfoodCredential save(IfoodCredential credential) {
        IfoodCredentialEntity entity = IfoodCredentialMapper.toEntity(credential);
        if (entity.getId() == null) {
            panacheRepo.persist(entity);
        } else {
            entity = panacheRepo.getEntityManager().merge(entity);
        }
        return IfoodCredentialMapper.toDomain(entity);
    }

    @Override
    public Optional<IfoodCredential> findByStoreId(UUID storeId) {
        return panacheRepo.findByStoreId(storeId)
                .map(IfoodCredentialMapper::toDomain);
    }

    @Override
    public List<IfoodCredential> findAllEnabled() {
        return panacheRepo.find("enabled", true).list()
                .stream()
                .map(IfoodCredentialMapper::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public void deleteByStoreId(UUID storeId) {
        panacheRepo.deleteByStoreId(storeId);
    }
}
