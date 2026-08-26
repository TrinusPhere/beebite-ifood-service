package com.fidecode.biteflowifood.domain.event;

import com.fidecode.biteflowifood.domain.event.valueobject.IfoodOrderStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

class IfoodOrderStatusTest {

    @ParameterizedTest
    @CsvSource({
            "PLC, PLACED",
            "CFM, CONFIRMED",
            "DSP, DISPATCHED",
            "CON, CONCLUDED",
            "CAN, CANCELLED",
            "RCA, CANCELLATION_REQUESTED",
            "RTP, READY_TO_PICKUP",
            "SPS, SEPARATION_STARTED"
    })
    void shouldParseAllIfoodStatusCodes(String code, String expected) {
        IfoodOrderStatus status = IfoodOrderStatus.fromCode(code);
        assertEquals(IfoodOrderStatus.valueOf(expected), status);
    }

    @Test
    void shouldThrowForUnknownCode() {
        assertThrows(IllegalArgumentException.class, () -> IfoodOrderStatus.fromCode("XYZ"));
    }

    @Test
    void shouldMapPlacedToBiteflowAnalise() {
        assertTrue(IfoodOrderStatus.PLACED.toBiteflowStatus().isPresent());
        assertEquals("ANALISE", IfoodOrderStatus.PLACED.toBiteflowStatus().get());
    }

    @Test
    void shouldMapDispatchedToBiteflowEmRota() {
        assertTrue(IfoodOrderStatus.DISPATCHED.toBiteflowStatus().isPresent());
        assertEquals("EM_ROTA", IfoodOrderStatus.DISPATCHED.toBiteflowStatus().get());
    }

    @Test
    void shouldMapConcludedToBiteflowFinalizado() {
        assertTrue(IfoodOrderStatus.CONCLUDED.toBiteflowStatus().isPresent());
        assertEquals("FINALIZADO", IfoodOrderStatus.CONCLUDED.toBiteflowStatus().get());
    }

    @Test
    void shouldMapCancelledToBiteflowCancelado() {
        assertTrue(IfoodOrderStatus.CANCELLED.toBiteflowStatus().isPresent());
        assertEquals("CANCELADO", IfoodOrderStatus.CANCELLED.toBiteflowStatus().get());
    }

    @Test
    void shouldReturnEmptyForIntermediaryStatuses() {
        assertTrue(IfoodOrderStatus.CONFIRMED.toBiteflowStatus().isEmpty());
        assertTrue(IfoodOrderStatus.READY_TO_PICKUP.toBiteflowStatus().isEmpty());
        assertTrue(IfoodOrderStatus.SEPARATION_STARTED.toBiteflowStatus().isEmpty());
        assertTrue(IfoodOrderStatus.CANCELLATION_REQUESTED.toBiteflowStatus().isEmpty());
    }
}
