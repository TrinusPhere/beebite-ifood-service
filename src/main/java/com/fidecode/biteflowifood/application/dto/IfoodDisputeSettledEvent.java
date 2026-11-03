package com.fidecode.biteflowifood.application.dto;

import java.util.Map;
import java.util.UUID;

public record IfoodDisputeSettledEvent(
        UUID storeId,
        String orderId,
        String disputeId,
        String eventId,
        String status,
        Map<String, Object> metadata
) {}
