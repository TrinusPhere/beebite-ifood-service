package com.trinusphere.beebiteifood.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "catalog_mapping")
public class CatalogMappingEntity {

    @Id
    @Column(name = "id")
    public UUID id;

    @Column(name = "store_id", nullable = false)
    public UUID storeId;

    @Column(name = "ifood_id", nullable = false, length = 50)
    public String ifoodId;

    @Column(name = "biteflow_id", nullable = false)
    public UUID biteflowId;

    @Column(name = "mapping_type", nullable = false, length = 20)
    public String mappingType;

    @Column(name = "created_at", nullable = false, updatable = false)
    public Instant createdAt;
}
