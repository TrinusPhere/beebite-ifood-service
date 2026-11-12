package com.fidecode.beebiteifood.application.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Contrato do evento consumido do tópico Pub/Sub: biteflow-order-status-changed.
 * Publicado pelo BiteFlow quando o operador muda o status de um pedido iFood.
 */
public record BiteflowOrderStatusChangedEvent(
        UUID storeId,
        UUID orderId,
        String ifoodOrderId,
        String newStatus,
        Instant changedAt
) {}
