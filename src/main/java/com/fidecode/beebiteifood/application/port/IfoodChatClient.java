package com.fidecode.beebiteifood.application.port;

import java.util.List;

/**
 * @deprecated Não existe API pública de chat no iFood.
 * Toda a experiência de chat é provida através do Widget JS oficial (https://widgets.ifood.com.br/widget.js).
 */
@Deprecated(forRemoval = true)
public interface IfoodChatClient {
    void sendMessage(String accessToken, String orderId, String message);
    List<IfoodChatMessage> getMessages(String accessToken, String orderId);

    record IfoodChatMessage(String id, String content, String sender, String createdAt) {}
}
