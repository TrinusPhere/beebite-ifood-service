package com.trinusphere.beebiteifood.infrastructure.persistence.mapper;

import com.trinusphere.beebiteifood.domain.catalog.entity.CatalogMapping;
import com.trinusphere.beebiteifood.infrastructure.persistence.entity.CatalogMappingEntity;

public final class CatalogMappingMapper {
    private CatalogMappingMapper() {}

    public static CatalogMapping toDomain(CatalogMappingEntity entity) {
        if (entity == null) return null;
        return CatalogMapping.reconstitute(
                entity.id,
                entity.storeId,
                entity.ifoodId,
                entity.biteflowId,
                entity.mappingType,
                entity.createdAt
        );
    }

    public static CatalogMappingEntity toEntity(CatalogMapping domain) {
        if (domain == null) return null;
        CatalogMappingEntity entity = new CatalogMappingEntity();
        entity.id = domain.getId();
        entity.storeId = domain.getStoreId();
        entity.ifoodId = domain.getIfoodId();
        entity.biteflowId = domain.getBiteflowId();
        entity.mappingType = domain.getMappingType();
        entity.createdAt = domain.getCreatedAt();
        return entity;
    }
}
