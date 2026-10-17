package com.fidecode.biteflowifood.application.port;

import java.util.List;

public interface IfoodChatClient {
    void sendMessage(String accessToken, String orderId, String message);
    List<IfoodChatMessage> getMessages(String accessToken, String orderId);

    record IfoodChatMessage(String id, String content, String sender, String createdAt) {}
}
