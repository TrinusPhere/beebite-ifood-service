package com.trinusphere.beebiteifood.domain.catalog.entity;

import java.time.Instant;
import java.util.UUID;

public class CatalogMapping {

    private UUID id;
    private final UUID catalogId;
    private final UUID storeId;
    private final String ifoodId;
    private final UUID biteflowId;
    private final String mappingType;
    private final Instant createdAt;

    public CatalogMapping(UUID storeId, String ifoodId, UUID biteflowId, String mappingType) {
        this(storeId, storeId, ifoodId, biteflowId, mappingType);
    }

    public CatalogMapping(UUID catalogId, UUID storeId, String ifoodId, UUID biteflowId, String mappingType) {
        this.id = UUID.randomUUID();
        this.catalogId = catalogId != null ? catalogId : storeId;
        this.storeId = storeId;
        this.ifoodId = ifoodId;
        this.biteflowId = biteflowId;
        this.mappingType = mappingType;
        this.createdAt = Instant.now();
    }

    private CatalogMapping(UUID id, UUID catalogId, UUID storeId, String ifoodId, UUID biteflowId, String mappingType, Instant createdAt) {
        this.id = id;
        this.catalogId = catalogId != null ? catalogId : storeId;
        this.storeId = storeId;
        this.ifoodId = ifoodId;
        this.biteflowId = biteflowId;
        this.mappingType = mappingType;
        this.createdAt = createdAt;
    }

    public static CatalogMapping reconstitute(UUID id, UUID storeId, String ifoodId, UUID biteflowId, String mappingType, Instant createdAt) {
        return new CatalogMapping(id, storeId, storeId, ifoodId, biteflowId, mappingType, createdAt);
    }

    public static CatalogMapping reconstitute(UUID id, UUID catalogId, UUID storeId, String ifoodId, UUID biteflowId, String mappingType, Instant createdAt) {
        return new CatalogMapping(id, catalogId, storeId, ifoodId, biteflowId, mappingType, createdAt);
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getCatalogId() {
        return catalogId;
    }

    public UUID getStoreId() {
        return storeId;
    }

    public String getIfoodId() {
        return ifoodId;
    }

    public UUID getBiteflowId() {
        return biteflowId;
    }

    public String getMappingType() {
        return mappingType;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
