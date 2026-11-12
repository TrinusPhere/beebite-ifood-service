package com.fidecode.beebiteifood.application.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Contrato do evento publicado no tópico Pub/Sub: ifood-order-status-changed.
 * Consumido pelo BiteFlow para atualizar o status do pedido no Kanban.
 */
public record IfoodOrderStatusChangedEvent(
        String eventId,
        UUID storeId,
        String ifoodOrderId,
        String previousStatus,
        String newBiteflowStatus,
        Instant changedAt
) {}
