package com.fidecode.beebiteifood.application.dto;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * Contrato do evento publicado no tópico Pub/Sub: ifood-order-placed.
 * Consumido pelo BiteFlow para criar o pedido no Kanban.
 * Inclui o confirmationDeadline calculado para observância do SLA de 8 minutos do iFood.
 */
public record IfoodOrderPlacedEvent(
        String eventId,
        UUID storeId,
        String ifoodOrderId,
        String displayId,
        String orderType,
        String orderTiming,
        String customerName,
        String customerPhone,
        String customerDocument,
        DeliveryAddress deliveryAddress,
        List<OrderItem> items,
        List<Payment> payments,
        BigDecimal subtotal,
        BigDecimal deliveryFee,
        BigDecimal totalAmount,
        Instant createdAt,
        Instant confirmationDeadline
) {

    public IfoodOrderPlacedEvent(String eventId, UUID storeId, String ifoodOrderId,
                                 String displayId, String orderType, String orderTiming,
                                 String customerName, String customerPhone, String customerDocument,
                                 DeliveryAddress deliveryAddress, List<OrderItem> items,
                                 List<Payment> payments, BigDecimal subtotal, BigDecimal deliveryFee,
                                 BigDecimal totalAmount, Instant createdAt) {
        this(eventId, storeId, ifoodOrderId, displayId, orderType, orderTiming,
                customerName, customerPhone, customerDocument, deliveryAddress, items,
                payments, subtotal, deliveryFee, totalAmount, createdAt,
                createdAt != null ? createdAt.plus(8, ChronoUnit.MINUTES) : null);
    }

    /**
     * Retorna se o prazo do SLA de confirmação do iFood (8 min) já foi excedido em determinado instante.
     */
    public boolean isConfirmationSlaViolated(Instant at) {
        return confirmationDeadline != null && at != null && at.isAfter(confirmationDeadline);
    }

    /**
     * Retorna se o prazo do SLA de confirmação do iFood (8 min) já foi excedido agora.
     */
    public boolean isConfirmationSlaViolated() {
        return isConfirmationSlaViolated(Instant.now());
    }

    /**
     * Retorna o tempo restante de SLA em segundos (ou 0 se já expirado).
     */
    public long remainingSlaSeconds() {
        if (confirmationDeadline == null) return 0;
        long seconds = Duration.between(Instant.now(), confirmationDeadline).toSeconds();
        return Math.max(0, seconds);
    }

    public record DeliveryAddress(
            String streetName,
            String streetNumber,
            String neighborhood,
            String complement,
            String city,
            String state,
            String postalCode,
            String reference,
            Double latitude,
            Double longitude
    ) {}

    public record OrderItem(
            String name,
            int quantity,
            BigDecimal unitPrice,
            BigDecimal totalPrice,
            String observations,
            List<ItemOption> options
    ) {}

    public record ItemOption(
            String name,
            int quantity,
            BigDecimal unitPrice
    ) {}

    public record Payment(
            String method,
            boolean prepaid,
            BigDecimal value
    ) {}
}
