package com.fidecode.biteflowifood.application.port;

import com.fidecode.biteflowifood.application.dto.IfoodCancellationReason;
import com.fidecode.biteflowifood.application.dto.IfoodOrderDetails;

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

    /** Solicita o cancelamento do pedido com um motivo. */
    void requestCancellation(String accessToken, String orderId, String cancellationCode);
}
