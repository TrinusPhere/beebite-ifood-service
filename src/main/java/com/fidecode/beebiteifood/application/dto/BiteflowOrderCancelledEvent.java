package com.fidecode.beebiteifood.application.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Contrato do evento consumido do tópico Pub/Sub: beebite-order-cancelled.
 * Publicado pelo BiteFlow quando o operador cancela um pedido iFood.
 */
public record BiteflowOrderCancelledEvent(
        UUID storeId,
        UUID orderId,
        String ifoodOrderId,
        String cancellationCode,
        String reason,
        Instant cancelledAt
) {}
