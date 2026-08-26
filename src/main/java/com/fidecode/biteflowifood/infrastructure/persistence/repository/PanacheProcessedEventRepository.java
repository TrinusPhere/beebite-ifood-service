package com.fidecode.biteflowifood.infrastructure.persistence.repository;

import com.fidecode.biteflowifood.domain.event.repository.ProcessedEventRepository;
import com.fidecode.biteflowifood.infrastructure.persistence.entity.ProcessedEventEntity;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.UUID;

/** Implementa o port de domínio ProcessedEventRepository. */
@ApplicationScoped
public class PanacheProcessedEventRepository implements ProcessedEventRepository {

    private final ProcessedEventPanacheRepo panacheRepo;

    @Inject
    public PanacheProcessedEventRepository(ProcessedEventPanacheRepo panacheRepo) {
        this.panacheRepo = panacheRepo;
    }

    @Override
    public boolean exists(String eventId) {
        return panacheRepo.existsById(eventId);
    }

    @Override
    @Transactional
    public void markProcessed(String eventId, UUID storeId) {
        panacheRepo.persist(new ProcessedEventEntity(eventId, storeId));
    }
}
