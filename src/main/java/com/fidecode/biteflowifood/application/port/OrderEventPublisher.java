package com.fidecode.biteflowifood.application.port;

import com.fidecode.biteflowifood.application.dto.IfoodOrderPlacedEvent;
import com.fidecode.biteflowifood.application.dto.IfoodOrderStatusChangedEvent;

/**
 * Port — OrderEventPublisher.
 * Abstração para publicação de eventos de pedidos iFood no message broker.
 * Implementado em infrastructure por PubSubOrderEventPublisher.
 */
public interface OrderEventPublisher {

    /** Publica evento de novo pedido recebido do iFood no tópico ifood-order-placed. */
    void publishOrderPlaced(IfoodOrderPlacedEvent event);

    /** Publica evento de mudança de status de pedido iFood no tópico ifood-order-status-changed. */
    void publishOrderStatusChanged(IfoodOrderStatusChangedEvent event);

    /** Publica evento de recebimento de mensagem no chat. */
    void publishChatMessage(com.fidecode.biteflowifood.application.dto.IfoodChatMessageReceivedEvent event);
}
