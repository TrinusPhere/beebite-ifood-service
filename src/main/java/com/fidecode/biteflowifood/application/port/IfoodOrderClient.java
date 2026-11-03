package com.fidecode.biteflowifood.application.port;

import com.fidecode.biteflowifood.application.dto.IfoodAcceptDisputeRequest;
import com.fidecode.biteflowifood.application.dto.IfoodAlternativeDisputeRequest;
import com.fidecode.biteflowifood.application.dto.IfoodCancellationReason;
import com.fidecode.biteflowifood.application.dto.IfoodOrderDetails;
import com.fidecode.biteflowifood.application.dto.IfoodRejectDisputeRequest;

import java.util.List;

/**
 * Port — IfoodOrderClient.
 * Abstração para operações de pedidos na API do iFood.
 * Implementado em infrastructure por IfoodOrderRestClient.
 */
public interface IfoodOrderClient {

    /** Busca os detalhes completos de um pedido pelo ID do iFood. */
    IfoodOrderDetails getOrderDetails(String accessToken, String orderId);

    /** Confirma o pedido (restaurante aceita o pedido). */
    void confirmOrder(String accessToken, String orderId);

    /** Inicia a preparação do pedido. */
    void startPreparation(String accessToken, String orderId);

    /** Marca o pedido como pronto para retirada. */
    void readyToPickup(String accessToken, String orderId);

    /** Despacha o pedido para entrega. */
    void dispatchOrder(String accessToken, String orderId);

    /** Lista os motivos de cancelamento disponíveis para o pedido. */
    List<IfoodCancellationReason> getCancellationReasons(String accessToken, String orderId);

    /**
     * Solicita o cancelamento do pedido. {@code cancellationCode} deve ser um {@code cancelCodeId}
     * retornado por {@link #getCancellationReasons}; {@code reason} é o texto livre do motivo.
     */
    void requestCancellation(String accessToken, String orderId, String cancellationCode, String reason);

    /**
     * Aceita a contestação/cancelamento aberto na Plataforma de Negociação (Handshake).
     * Endpoint: POST /order/v1.0/disputes/{disputeId}/accept
     */
    void acceptDispute(String accessToken, String disputeId, IfoodAcceptDisputeRequest request);

    /**
     * Rejeita a contestação/cancelamento aberto na Plataforma de Negociação (Handshake).
     * Endpoint: POST /order/v1.0/disputes/{disputeId}/reject
     */
    void rejectDispute(String accessToken, String disputeId, IfoodRejectDisputeRequest request);

    /**
     * Propõe uma alternativa para a disputa (tempo adicional ou reembolso/benefício).
     * Endpoint: POST /order/v1.0/disputes/{disputeId}/alternative
     */
    void proposeDisputeAlternative(String accessToken, String disputeId, IfoodAlternativeDisputeRequest request);
}
