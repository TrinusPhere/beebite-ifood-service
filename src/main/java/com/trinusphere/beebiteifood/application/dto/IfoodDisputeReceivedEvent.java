package com.trinusphere.beebiteifood.application.dto;

import java.util.Map;
import java.util.UUID;

public record IfoodDisputeReceivedEvent(
        UUID storeId,
        String orderId,
        String disputeId,
        String eventId,
        String handshakeType,
        String expiresAt,
        String timeoutAction,
        Map<String, Object> metadata
) {}
