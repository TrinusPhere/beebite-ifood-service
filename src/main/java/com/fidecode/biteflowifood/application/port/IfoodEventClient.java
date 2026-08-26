package com.fidecode.biteflowifood.application.port;

import com.fidecode.biteflowifood.domain.event.entity.IfoodEvent;

import java.util.List;

/**
 * Port — IfoodEventClient.
 * Abstração para operações de eventos na API do iFood.
 * Implementado em infrastructure por IfoodEventRestClient.
 */
public interface IfoodEventClient {

    /**
     * Faz polling de eventos pendentes do iFood.
     * Retorna lista vazia se não houver eventos (HTTP 204).
     */
    List<IfoodEvent> pollEvents(String accessToken, String merchantId);

    /**
     * Confirma (ACK) o processamento dos eventos ao iFood.
     * Deve ser chamado após processar TODOS os eventos do ciclo.
     */
    void acknowledgeEvents(String accessToken, List<String> eventIds);
}
