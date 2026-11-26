package com.trinusphere.beebiteifood.domain.catalog.repository;

import com.trinusphere.beebiteifood.domain.catalog.entity.CatalogMapping;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CatalogMappingRepository {
    void save(CatalogMapping mapping);
    void saveAll(List<CatalogMapping> mappings);
    Optional<CatalogMapping> findByIfoodIdAndCatalogId(String ifoodId, UUID catalogId);
    Optional<CatalogMapping> findByBiteflowIdAndCatalogId(UUID biteflowId, UUID catalogId);
    List<CatalogMapping> findByCatalogId(UUID catalogId);
    void deleteByCatalogId(UUID catalogId);
    Optional<CatalogMapping> findByIfoodIdAndStoreId(String ifoodId, UUID storeId);
    Optional<CatalogMapping> findByBiteflowIdAndStoreId(UUID biteflowId, UUID storeId);
    List<CatalogMapping> findByStoreId(UUID storeId);
    void deleteByStoreId(UUID storeId);
    void delete(CatalogMapping mapping);
}
