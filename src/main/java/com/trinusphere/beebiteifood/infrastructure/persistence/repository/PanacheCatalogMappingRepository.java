package com.trinusphere.beebiteifood.infrastructure.persistence.repository;

import com.trinusphere.beebiteifood.domain.catalog.entity.CatalogMapping;
import com.trinusphere.beebiteifood.domain.catalog.repository.CatalogMappingRepository;
import com.trinusphere.beebiteifood.infrastructure.persistence.entity.CatalogMappingEntity;
import com.trinusphere.beebiteifood.infrastructure.persistence.mapper.CatalogMappingMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class PanacheCatalogMappingRepository implements CatalogMappingRepository {

    @Inject
    CatalogMappingPanacheRepo panacheRepo;

    @Override
    @Transactional
    public void save(CatalogMapping mapping) {
        UUID catalogId = mapping.getCatalogId() != null ? mapping.getCatalogId() : mapping.getStoreId();
        Optional<CatalogMappingEntity> existing = panacheRepo
                .find("catalogId = ?1 and ifoodId = ?2 and mappingType = ?3",
                        catalogId, mapping.getIfoodId(), mapping.getMappingType())
                .firstResultOptional();
        if (existing.isPresent()) {
            CatalogMappingEntity entity = existing.get();
            entity.storeId = mapping.getStoreId();
            entity.biteflowId = mapping.getBiteflowId();
            panacheRepo.getEntityManager().merge(entity);
        } else {
            panacheRepo.getEntityManager().merge(CatalogMappingMapper.toEntity(mapping));
        }
    }

    @Override
    @Transactional
    public void saveAll(List<CatalogMapping> mappings) {
        mappings.forEach(this::save);
    }

    @Override
    public Optional<CatalogMapping> findByIfoodIdAndCatalogId(String ifoodId, UUID catalogId) {
        return panacheRepo.find("ifoodId = ?1 and catalogId = ?2", ifoodId, catalogId)
                .firstResultOptional()
                .map(CatalogMappingMapper::toDomain);
    }

    @Override
    public Optional<CatalogMapping> findByBiteflowIdAndCatalogId(UUID biteflowId, UUID catalogId) {
        return panacheRepo.find("biteflowId = ?1 and catalogId = ?2", biteflowId, catalogId)
                .firstResultOptional()
                .map(CatalogMappingMapper::toDomain);
    }

    @Override
    public List<CatalogMapping> findByCatalogId(UUID catalogId) {
        return panacheRepo.find("catalogId = ?1", catalogId)
                .stream()
                .map(CatalogMappingMapper::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public void deleteByCatalogId(UUID catalogId) {
        panacheRepo.delete("catalogId = ?1", catalogId);
    }

    @Override
    public Optional<CatalogMapping> findByIfoodIdAndStoreId(String ifoodId, UUID storeId) {
        return panacheRepo.find("ifoodId = ?1 and storeId = ?2", ifoodId, storeId)
                .firstResultOptional()
                .map(CatalogMappingMapper::toDomain);
    }

    @Override
    public Optional<CatalogMapping> findByBiteflowIdAndStoreId(UUID biteflowId, UUID storeId) {
        return panacheRepo.find("biteflowId = ?1 and storeId = ?2", biteflowId, storeId)
                .firstResultOptional()
                .map(CatalogMappingMapper::toDomain);
    }

    @Override
    public List<CatalogMapping> findByStoreId(UUID storeId) {
        return panacheRepo.find("storeId = ?1", storeId)
                .stream()
                .map(CatalogMappingMapper::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public void deleteByStoreId(UUID storeId) {
        panacheRepo.delete("storeId = ?1", storeId);
    }

    @Override
    @Transactional
    public void delete(CatalogMapping mapping) {
        if (mapping.getCatalogId() != null) {
            panacheRepo.delete("ifoodId = ?1 and catalogId = ?2", mapping.getIfoodId(), mapping.getCatalogId());
        } else {
            panacheRepo.delete("ifoodId = ?1 and storeId = ?2", mapping.getIfoodId(), mapping.getStoreId());
        }
    }
}
