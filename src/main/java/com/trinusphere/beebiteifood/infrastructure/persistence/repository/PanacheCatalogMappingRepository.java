package com.trinusphere.beebiteifood.infrastructure.persistence.repository;

import com.trinusphere.beebiteifood.domain.catalog.entity.CatalogMapping;
import com.trinusphere.beebiteifood.domain.catalog.repository.CatalogMappingRepository;
import com.trinusphere.beebiteifood.infrastructure.persistence.mapper.CatalogMappingMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.transaction.Transactional;

@ApplicationScoped
public class PanacheCatalogMappingRepository implements CatalogMappingRepository {

    @Inject
    CatalogMappingPanacheRepo panacheRepo;

    @Override
    @Transactional
    public void save(CatalogMapping mapping) {
        panacheRepo.getEntityManager().merge(CatalogMappingMapper.toEntity(mapping));
    }

    @Override
    @Transactional
    public void saveAll(List<CatalogMapping> mappings) {
        mappings.forEach(this::save);
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
        panacheRepo.delete("ifoodId = ?1 and storeId = ?2", mapping.getIfoodId(), mapping.getStoreId());
    }
}
