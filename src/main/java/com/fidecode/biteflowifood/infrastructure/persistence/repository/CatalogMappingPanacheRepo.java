package com.fidecode.biteflowifood.infrastructure.persistence.repository;

import com.fidecode.biteflowifood.infrastructure.persistence.entity.CatalogMappingEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.UUID;

@ApplicationScoped
public class CatalogMappingPanacheRepo implements PanacheRepositoryBase<CatalogMappingEntity, UUID> {
}
