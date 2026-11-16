package com.trinusphere.beebiteifood.application.port;

import com.trinusphere.beebiteifood.application.dto.IfoodOrderPlacedEvent;
import com.trinusphere.beebiteifood.application.dto.IfoodOrderStatusChangedEvent;

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

    /**
     * @deprecated O chat oficial é oferecido exclusivamente via Widget JS do iFood.
     */
    @Deprecated
    void publishChatMessage(com.trinusphere.beebiteifood.application.dto.IfoodChatMessageReceivedEvent event);

    /** Publica evento de disputa/negociação aberta pelo cliente ou iFood (HSD). */
    void publishDisputeReceived(com.trinusphere.beebiteifood.application.dto.IfoodDisputeReceivedEvent event);

    /** Publica evento de resolução/encerramento de disputa (HSS). */
    void publishDisputeSettled(com.trinusphere.beebiteifood.application.dto.IfoodDisputeSettledEvent event);

    /** Publica evento de solicitação de cancelamento pelo cliente (CAR). */
    void publishCancellationRequested(com.trinusphere.beebiteifood.application.dto.IfoodCancellationRequestedEvent event);
}
