package com.fidecode.biteflowifood.infrastructure.persistence.repository;

import com.fidecode.biteflowifood.domain.catalog.entity.CatalogMapping;
import com.fidecode.biteflowifood.domain.catalog.repository.CatalogMappingRepository;
import com.fidecode.biteflowifood.infrastructure.persistence.mapper.CatalogMappingMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class PanacheCatalogMappingRepository implements CatalogMappingRepository {

    @Inject
    CatalogMappingPanacheRepo panacheRepo;

    @Override
    public void save(CatalogMapping mapping) {
        panacheRepo.getEntityManager().merge(CatalogMappingMapper.toEntity(mapping));
    }

    @Override
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
    public void deleteByStoreId(UUID storeId) {
        panacheRepo.delete("storeId = ?1", storeId);
    }
}
