package com.fidecode.biteflowifood.application.usecase;

import com.fidecode.biteflowifood.application.dto.IfoodOrderDetails;
import com.fidecode.biteflowifood.application.dto.IfoodOrderPlacedEvent;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Mapper estático — converte IfoodOrderDetails → IfoodOrderPlacedEvent.
 * Segue P6 (DRY): centraliza a conversão em um único lugar.
 * Calcula o confirmationDeadline com base no SLA de 8 minutos oficial do iFood.
 */
final class IfoodEventMapper {

    private IfoodEventMapper() {}

    public static IfoodOrderPlacedEvent toPlacedEvent(
            IfoodOrderDetails details, UUID storeId, String eventId) {

        IfoodOrderPlacedEvent.DeliveryAddress address = null;
        if (details.delivery() != null && details.delivery().deliveryAddress() != null) {
            IfoodOrderDetails.IfoodAddress a = details.delivery().deliveryAddress();
            address = new IfoodOrderPlacedEvent.DeliveryAddress(
                    a.streetName(), a.streetNumber(), a.neighborhood(),
                    a.complement(), a.city(), a.state(), a.postalCode(),
                    a.reference(), a.latitude(), a.longitude()
            );
        }

        List<IfoodOrderPlacedEvent.OrderItem> items = Collections.emptyList();
        if (details.items() != null) {
            items = details.items().stream()
                    .map(i -> new IfoodOrderPlacedEvent.OrderItem(
                            i.name(), i.quantity(), i.unitPrice(), i.totalPrice(),
                            i.observations(),
                            i.options() == null ? Collections.emptyList() :
                                    i.options().stream()
                                            .map(o -> new IfoodOrderPlacedEvent.ItemOption(o.name(), o.quantity(), o.unitPrice()))
                                            .toList()
                    )).toList();
        }

        List<IfoodOrderPlacedEvent.Payment> payments = Collections.emptyList();
        if (details.payments() != null && details.payments().methods() != null) {
            payments = details.payments().methods().stream()
                    .map(p -> new IfoodOrderPlacedEvent.Payment(p.method(), p.prepaid(), p.value()))
                    .toList();
        }

        BigDecimal deliveryFee = details.total() != null ? details.total().deliveryFee() : BigDecimal.ZERO;
        BigDecimal totalAmount = details.total() != null ? details.total().orderAmount() : BigDecimal.ZERO;

        String customerName = details.customer() != null ? details.customer().name() : null;
        String customerPhone = (details.customer() != null && details.customer().phone() != null) ? details.customer().phone().number() : null;
        String customerDoc = details.customer() != null ? details.customer().document() : null;

        // Extrai createdAt oficial retornado pelo iFood em UTC
        Instant createdAt = null;
        if (details.createdAt() != null && !details.createdAt().isBlank()) {
            try {
                createdAt = Instant.parse(details.createdAt());
            } catch (Exception ignored) {}
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }

        // SLA de confirmação oficial do iFood: 8 minutos
        // - IMMEDIATE: 8 minutos a partir do createdAt
        // - SCHEDULED: 8 minutos a partir do preparationStartDateTime
        Instant confirmationDeadline;
        if ("SCHEDULED".equalsIgnoreCase(details.orderTiming())
                && details.schedule() != null
                && details.schedule().preparationStartDateTime() != null
                && !details.schedule().preparationStartDateTime().isBlank()) {
            try {
                Instant prepStart = Instant.parse(details.schedule().preparationStartDateTime());
                confirmationDeadline = prepStart.plus(8, ChronoUnit.MINUTES);
            } catch (Exception e) {
                confirmationDeadline = createdAt.plus(8, ChronoUnit.MINUTES);
            }
        } else {
            confirmationDeadline = createdAt.plus(8, ChronoUnit.MINUTES);
        }

        return new IfoodOrderPlacedEvent(
                eventId, storeId, details.id(), details.displayId(),
                details.orderType(), details.orderTiming(),
                customerName, customerPhone, customerDoc,
                address, items, payments,
                details.subTotal(), deliveryFee, totalAmount,
                createdAt, confirmationDeadline
        );
    }
}
