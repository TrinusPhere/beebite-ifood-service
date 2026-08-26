package com.fidecode.biteflowifood.domain.event.valueobject;

import java.util.Map;
import java.util.Optional;

/**
 * Value Object — Códigos de status de eventos da API do iFood.
 * Mapeia os códigos curtos (PLC, CFM, etc.) para os nomes semânticos
 * e para os status correspondentes no BiteFlow (quando aplicável).
 */
public enum IfoodOrderStatus {

    PLACED,               // PLC — Pedido recebido (novo pedido)
    CONFIRMED,            // CFM — Pedido confirmado pelo restaurante
    SEPARATION_STARTED,   // SPS — Separação iniciada
    READY_TO_PICKUP,      // RTP — Pronto para retirada
    DISPATCHED,           // DSP — Saiu para entrega
    CONCLUDED,            // CON — Concluído
    CANCELLED,            // CAN — Cancelado
    CANCELLATION_REQUESTED; // RCA — Cancelamento solicitado pelo cliente

    private static final Map<String, IfoodOrderStatus> CODE_MAP = Map.of(
            "PLC", PLACED,
            "CFM", CONFIRMED,
            "SPS", SEPARATION_STARTED,
            "RTP", READY_TO_PICKUP,
            "DSP", DISPATCHED,
            "CON", CONCLUDED,
            "CAN", CANCELLED,
            "RCA", CANCELLATION_REQUESTED
    );

    /**
     * Converte o código curto do iFood (ex: "PLC") para o enum.
     *
     * @throws IllegalArgumentException se o código for desconhecido
     */
    public static IfoodOrderStatus fromCode(String code) {
        IfoodOrderStatus status = CODE_MAP.get(code);
        if (status == null) {
            throw new IllegalArgumentException("Código de status iFood desconhecido: " + code);
        }
        return status;
    }

    /**
     * Retorna o status BiteFlow correspondente, se houver.
     * Eventos intermediários (CFM, SPS, RTP, RCA) não mudam o status no BiteFlow.
     */
    public Optional<String> toBiteflowStatus() {
        return switch (this) {
            case PLACED -> Optional.of("ANALISE");
            case DISPATCHED -> Optional.of("EM_ROTA");
            case CONCLUDED -> Optional.of("FINALIZADO");
            case CANCELLED -> Optional.of("CANCELADO");
            default -> Optional.empty();
        };
    }
}
