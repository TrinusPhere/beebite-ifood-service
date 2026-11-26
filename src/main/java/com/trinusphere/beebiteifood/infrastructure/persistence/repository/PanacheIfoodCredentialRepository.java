package com.trinusphere.beebiteifood.infrastructure.persistence.repository;

import com.trinusphere.beebiteifood.domain.credential.entity.IfoodCredential;
import com.trinusphere.beebiteifood.domain.credential.repository.IfoodCredentialRepository;
import com.trinusphere.beebiteifood.infrastructure.persistence.entity.IfoodCredentialEntity;
import com.trinusphere.beebiteifood.infrastructure.persistence.mapper.IfoodCredentialMapper;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
    public Optional<IfoodCredential> findByCatalogId(UUID catalogId) {
        if (catalogId == null) {
            return Optional.empty();
        }
        return panacheRepo.findByCatalogId(catalogId)
                .map(IfoodCredentialMapper::toDomain);
    }

    @Override
    public Optional<IfoodCredential> findByMerchantId(String merchantId) {
        if (merchantId == null || merchantId.isBlank()) {
            return Optional.empty();
        }
        return panacheRepo.findByMerchantId(merchantId.trim())
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
