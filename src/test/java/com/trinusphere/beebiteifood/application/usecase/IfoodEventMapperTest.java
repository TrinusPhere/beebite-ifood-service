package com.trinusphere.beebiteifood.application.usecase;

import com.trinusphere.beebiteifood.application.dto.IfoodOrderDetails;
import com.trinusphere.beebiteifood.application.dto.IfoodOrderPlacedEvent;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class IfoodEventMapperTest {

    private final UUID storeId = UUID.randomUUID();

    @Test
    void shouldCalculate8MinuteSlaForImmediateOrders() {
        Instant created = Instant.parse("2026-10-22T10:00:00Z");
        IfoodOrderDetails details = buildDetails("ord-1", "IMMEDIATE", created.toString(), null);

        IfoodOrderPlacedEvent event = IfoodEventMapper.toPlacedEvent(details, storeId, "evt-1");

        assertEquals(created, event.createdAt());
        assertEquals(created.plus(8, ChronoUnit.MINUTES), event.confirmationDeadline());
        assertFalse(event.isConfirmationSlaViolated(created.plus(5, ChronoUnit.MINUTES)));
        assertTrue(event.isConfirmationSlaViolated(created.plus(9, ChronoUnit.MINUTES)));
    }

    @Test
    void shouldCalculate8MinuteSlaFromPreparationStartForScheduledOrders() {
        Instant created = Instant.parse("2026-10-22T10:00:00Z");
        Instant prepStart = Instant.parse("2026-10-22T12:00:00Z");
        IfoodOrderDetails.IfoodSchedule schedule = new IfoodOrderDetails.IfoodSchedule(
                "2026-10-22T12:30:00Z", "2026-10-22T13:00:00Z", prepStart.toString()
        );
        IfoodOrderDetails details = buildDetails("ord-2", "SCHEDULED", created.toString(), schedule);

        IfoodOrderPlacedEvent event = IfoodEventMapper.toPlacedEvent(details, storeId, "evt-2");

        assertEquals(created, event.createdAt());
        assertEquals(prepStart.plus(8, ChronoUnit.MINUTES), event.confirmationDeadline());
    }

    @Test
    void shouldFallbackToCreatedAtWhenSchedulePreparationStartIsMissing() {
        Instant created = Instant.parse("2026-10-22T10:00:00Z");
        IfoodOrderDetails.IfoodSchedule schedule = new IfoodOrderDetails.IfoodSchedule(null, null, null);
        IfoodOrderDetails details = buildDetails("ord-3", "SCHEDULED", created.toString(), schedule);

        IfoodOrderPlacedEvent event = IfoodEventMapper.toPlacedEvent(details, storeId, "evt-3");

        assertEquals(created.plus(8, ChronoUnit.MINUTES), event.confirmationDeadline());
    }

    private IfoodOrderDetails buildDetails(String id, String timing, String createdAt, IfoodOrderDetails.IfoodSchedule schedule) {
        return new IfoodOrderDetails(
                id, "N1", null, createdAt, "DELIVERY", timing,
                new IfoodOrderDetails.IfoodMerchantRef("m1", "Store"),
                new IfoodOrderDetails.IfoodCustomer("c1", "Maria", null, null, null),
                List.of(), BigDecimal.TEN, BigDecimal.ZERO, List.of(),
                new IfoodOrderDetails.IfoodTotal(BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO),
                new IfoodOrderDetails.IfoodPayments(BigDecimal.TEN, BigDecimal.ZERO, List.of()),
                null, schedule
        );
    }
}
