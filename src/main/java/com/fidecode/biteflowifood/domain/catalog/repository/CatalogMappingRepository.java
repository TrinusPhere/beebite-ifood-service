package com.fidecode.biteflowifood.domain.catalog.repository;

import com.fidecode.biteflowifood.domain.catalog.entity.CatalogMapping;

import java.util.Optional;
import java.util.UUID;
import java.util.List;

public interface CatalogMappingRepository {
    void save(CatalogMapping mapping);
    void saveAll(List<CatalogMapping> mappings);
    Optional<CatalogMapping> findByIfoodIdAndStoreId(String ifoodId, UUID storeId);
    Optional<CatalogMapping> findByBiteflowIdAndStoreId(UUID biteflowId, UUID storeId);
    List<CatalogMapping> findByStoreId(UUID storeId);
    void deleteByStoreId(UUID storeId);
}
