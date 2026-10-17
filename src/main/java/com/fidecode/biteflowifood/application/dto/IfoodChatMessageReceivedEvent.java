package com.fidecode.biteflowifood.application.dto;

import java.time.Instant;

public record IfoodChatMessageReceivedEvent(
        String storeId,
        String orderId,
        String messageId,
        String content,
        String sender,
        Instant receivedAt
) {}
