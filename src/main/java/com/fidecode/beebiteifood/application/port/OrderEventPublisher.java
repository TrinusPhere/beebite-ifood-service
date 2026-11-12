package com.fidecode.beebiteifood.application.port;

import com.fidecode.beebiteifood.application.dto.IfoodOrderPlacedEvent;
import com.fidecode.beebiteifood.application.dto.IfoodOrderStatusChangedEvent;

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
    void publishChatMessage(com.fidecode.beebiteifood.application.dto.IfoodChatMessageReceivedEvent event);

    /** Publica evento de disputa/negociação aberta pelo cliente ou iFood (HSD). */
    void publishDisputeReceived(com.fidecode.beebiteifood.application.dto.IfoodDisputeReceivedEvent event);

    /** Publica evento de resolução/encerramento de disputa (HSS). */
    void publishDisputeSettled(com.fidecode.beebiteifood.application.dto.IfoodDisputeSettledEvent event);

    /** Publica evento de solicitação de cancelamento pelo cliente (CAR). */
    void publishCancellationRequested(com.fidecode.beebiteifood.application.dto.IfoodCancellationRequestedEvent event);
}
