package com.fidecode.biteflowifood.application.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record IfoodCancellationRequestedEvent(
        UUID storeId,
        String orderId,
        String eventId,
        String reasonCode,
        String details,
        Map<String, Object> metadata,
        Instant requestedAt
) {}
