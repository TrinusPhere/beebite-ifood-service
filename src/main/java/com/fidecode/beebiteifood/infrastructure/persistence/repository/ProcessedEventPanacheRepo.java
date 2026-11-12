package com.fidecode.beebiteifood.infrastructure.persistence.repository;

import com.fidecode.beebiteifood.infrastructure.persistence.entity.ProcessedEventEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;

import jakarta.enterprise.context.ApplicationScoped;

/** Repositório Panache auxiliar para rastreamento de eventos processados. */
@ApplicationScoped
public class ProcessedEventPanacheRepo implements PanacheRepositoryBase<ProcessedEventEntity, String> {

    public boolean existsById(String eventId) {
        return count("eventId", eventId) > 0;
    }
}
