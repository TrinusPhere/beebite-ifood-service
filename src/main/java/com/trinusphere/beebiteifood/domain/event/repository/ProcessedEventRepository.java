package com.trinusphere.beebiteifood.domain.event.repository;

import java.util.UUID;

/**
 * Port (Interface) — ProcessedEventRepository.
 * Garante idempotência: evita processar o mesmo evento iFood duas vezes.
 */
public interface ProcessedEventRepository {

    /** Retorna true se o evento já foi processado anteriormente. */
    boolean exists(String eventId);

    /** Marca o evento como processado. */
    void markProcessed(String eventId, UUID storeId);
}
