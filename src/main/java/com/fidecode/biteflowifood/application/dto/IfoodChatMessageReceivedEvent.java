package com.fidecode.biteflowifood.application.dto;

import java.time.Instant;

/**
 * @deprecated Não há API pública de chat no iFood. O chat é operado exclusivamente pelo Widget oficial no frontend.
 */
@Deprecated(forRemoval = true)
public record IfoodChatMessageReceivedEvent(
        String storeId,
        String orderId,
        String messageId,
        String content,
        String sender,
        Instant receivedAt
) {}
