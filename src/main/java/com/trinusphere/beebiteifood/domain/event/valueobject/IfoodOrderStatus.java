package com.trinusphere.beebiteifood.domain.event.valueobject;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Value Object — Códigos de eventos da API de Events do iFood.
 * Fonte: integracao_ifood/Events/eventos_de_pedido.md
 *
 * Cada constante carrega o código curto enviado pelo iFood (PLC, CFM, ...).
 * Apenas PLACED, DISPATCHED, CONCLUDED e CANCELLED alteram o status no BiteFlow;
 * os demais são reconhecidos (para não gerar alertas de "código desconhecido"),
 * mas não produzem mudança de status.
 */
public enum IfoodOrderStatus {

    // ── ORDER_STATUS ──────────────────────────────────────────────────────────
    PLACED("PLC"),                 // Novo pedido na plataforma
    CONFIRMED("CFM"),              // Pedido confirmado e será preparado
    SEPARATION_STARTED("SPS"),     // Início do processo de separação
    SEPARATION_ENDED("SPE"),       // Conclusão da separação
    READY_TO_PICKUP("RTP"),        // Pronto para ser retirado
    DISPATCHED("DSP"),             // Saiu para entrega
    CONCLUDED("CON"),              // Concluído
    CANCELLED("CAN"),              // Cancelado

    // ── CANCELLATION_REQUEST ──────────────────────────────────────────────────
    CANCELLATION_REQUESTED("CAR"),       // Solicitação de cancelamento feita
    CANCELLATION_REQUEST_FAILED("CARF"), // Solicitação de cancelamento negada

    // ── HANDSHAKE_PLATFORM ────────────────────────────────────────────────────
    HANDSHAKE_DISPUTE("HSD"),      // Negociação iniciada
    HANDSHAKE_SETTLEMENT("HSS"),   // Negociação respondida

    // ── DELIVERY ──────────────────────────────────────────────────────────────
    ASSIGN_DRIVER("ADR"),
    GOING_TO_ORIGIN("GTO"),
    ARRIVED_AT_ORIGIN("AAO"),
    DELIVERY_DRIVER_DEALLOCATED("DDD"),
    COLLECTED("CLT"),
    ARRIVED_AT_DESTINATION("AAD"),
    DELIVERY_RETURNING_TO_ORIGIN("DRGO"),
    DELIVERY_RETURNED_TO_ORIGIN("DRDO"),
    DELIVERY_RETURN_CODE_REQUESTED("DRCR"),
    DELIVERY_DISPUTE_CODE_REQUESTED("DDCR"),

    // ── DELIVERY_ADDRESS (pedidos sob demanda) ────────────────────────────────
    DELIVERY_ADDRESS_CHANGE_REQUESTED("DAR"),
    DELIVERY_ADDRESS_CHANGE_USER_CONFIRMED("DAU"),
    DELIVERY_ADDRESS_CHANGE_ACCEPTED("DAA"),
    DELIVERY_ADDRESS_CHANGE_DENIED("DAD"),

    // ── DELIVERY_GROUP ────────────────────────────────────────────────────────
    DELIVERY_GROUP_ASSIGNED("DGA"),
    DELIVERY_GROUP_DISMISSED("DGD"),
    DELIVERY_GROUP_ASSOCIATED("DGAC"),
    DELIVERY_GROUP_DISSOCIATED("DGDC"),
    DELIVERY_GROUP_UPDATED("DGU"),

    // ── DELIVERY_ONDEMAND ─────────────────────────────────────────────────────
    REQUEST_DRIVER("RDR"),
    REQUEST_DRIVER_SUCCESS("RDS"),
    REQUEST_DRIVER_FAILED("RDF"),
    DELIVERY_CANCELLATION_REQUEST_ACCEPTED("DCRA"),
    DELIVERY_CANCELLATION_REQUEST_REJECTED("DCRR"),

    // ── DELIVERY_COMPLEMENT ───────────────────────────────────────────────────
    RETURN_TO_STORE("RTS"),

    // ── OUTROS ────────────────────────────────────────────────────────────────
    ORDER_PATCHED("OPA"),
    RECOMMENDED_PREPARATION_START("RPS"),
    PREPARATION_STARTED("PRS"),
    CONSUMER_PREPARATION_TIME_REQUESTED("CPR"),
    CHANGE_PREPARATION_TIME("CPT"),
    BOX_ASSIGNED("BOA"),
    READY_FOR_INVOICE("RFI");

    private static final Map<String, IfoodOrderStatus> BY_CODE = new HashMap<>();

    static {
        for (IfoodOrderStatus status : values()) {
            BY_CODE.put(status.code, status);
        }
    }

    private final String code;

    IfoodOrderStatus(String code) {
        this.code = code;
    }

    /** Código curto do evento no iFood (ex: "PLC"). */
    public String code() {
        return code;
    }

    /**
     * Converte o código curto do iFood (ex: "PLC") para o enum.
     *
     * @throws IllegalArgumentException se o código for nulo ou desconhecido
     */
    public static IfoodOrderStatus fromCode(String code) {
        IfoodOrderStatus status = code == null ? null : BY_CODE.get(code);
        if (status == null) {
            throw new IllegalArgumentException("Código de status iFood desconhecido: " + code);
        }
        return status;
    }

    /**
     * Retorna o status BiteFlow correspondente, se houver.
     * Eventos intermediários (CFM, SPS, SPE, RTP, CAR, CARF, ...) não mudam o status no BiteFlow.
     */
    public Optional<String> toBiteflowStatus() {
        return switch (this) {
            case PLACED -> Optional.of("ANALISE");
            case CONFIRMED -> Optional.of("PREPARO");
            case DISPATCHED -> Optional.of("EM_ROTA");
            case CONCLUDED -> Optional.of("FINALIZADO");
            case CANCELLED -> Optional.of("CANCELADO");
            default -> Optional.empty();
        };
    }
}
