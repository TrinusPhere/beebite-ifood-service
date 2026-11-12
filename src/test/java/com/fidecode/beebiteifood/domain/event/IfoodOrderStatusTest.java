package com.fidecode.beebiteifood.domain.event;

import com.fidecode.beebiteifood.domain.event.valueobject.IfoodOrderStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class IfoodOrderStatusTest {

    @ParameterizedTest
    @CsvSource({
            "PLC, PLACED",
            "CFM, CONFIRMED",
            "DSP, DISPATCHED",
            "CON, CONCLUDED",
            "CAN, CANCELLED",
            "CAR, CANCELLATION_REQUESTED",
            "RTP, READY_TO_PICKUP",
            "SPS, SEPARATION_STARTED"
    })
    void shouldParseAllIfoodStatusCodes(String code, String expected) {
        IfoodOrderStatus status = IfoodOrderStatus.fromCode(code);
        assertEquals(IfoodOrderStatus.valueOf(expected), status);
    }

    /** Códigos oficiais: integracao_ifood/Events/eventos_de_pedido.md */
    @ParameterizedTest
    @CsvSource({
            // ORDER_STATUS
            "SPE, SEPARATION_ENDED",
            // CANCELLATION_REQUEST
            "CARF, CANCELLATION_REQUEST_FAILED",
            // HANDSHAKE_PLATFORM
            "HSD, HANDSHAKE_DISPUTE",
            "HSS, HANDSHAKE_SETTLEMENT",
            // DELIVERY
            "ADR, ASSIGN_DRIVER",
            "GTO, GOING_TO_ORIGIN",
            "AAO, ARRIVED_AT_ORIGIN",
            "DDD, DELIVERY_DRIVER_DEALLOCATED",
            "CLT, COLLECTED",
            "AAD, ARRIVED_AT_DESTINATION",
            "DRGO, DELIVERY_RETURNING_TO_ORIGIN",
            "DRDO, DELIVERY_RETURNED_TO_ORIGIN",
            "DRCR, DELIVERY_RETURN_CODE_REQUESTED",
            // DELIVERY_ADDRESS
            "DAR, DELIVERY_ADDRESS_CHANGE_REQUESTED",
            "DAU, DELIVERY_ADDRESS_CHANGE_USER_CONFIRMED",
            "DAA, DELIVERY_ADDRESS_CHANGE_ACCEPTED",
            "DAD, DELIVERY_ADDRESS_CHANGE_DENIED",
            // DELIVERY_GROUP
            "DGA, DELIVERY_GROUP_ASSIGNED",
            "DGD, DELIVERY_GROUP_DISMISSED",
            "DGAC, DELIVERY_GROUP_ASSOCIATED",
            "DGDC, DELIVERY_GROUP_DISSOCIATED",
            "DGU, DELIVERY_GROUP_UPDATED",
            // DELIVERY_ONDEMAND
            "RDR, REQUEST_DRIVER",
            "RDS, REQUEST_DRIVER_SUCCESS",
            "RDF, REQUEST_DRIVER_FAILED",
            "DCRA, DELIVERY_CANCELLATION_REQUEST_ACCEPTED",
            "DCRR, DELIVERY_CANCELLATION_REQUEST_REJECTED",
            // DELIVERY_COMPLEMENT
            "RTS, RETURN_TO_STORE",
            // OUTROS
            "OPA, ORDER_PATCHED",
            "RPS, RECOMMENDED_PREPARATION_START",
            "PRS, PREPARATION_STARTED",
            "CPR, CONSUMER_PREPARATION_TIME_REQUESTED",
            "CPT, CHANGE_PREPARATION_TIME",
            "BOA, BOX_ASSIGNED",
            "RFI, READY_FOR_INVOICE"
    })
    void shouldParseAllDocumentedEventCodes(String code, String expected) {
        assertEquals(IfoodOrderStatus.valueOf(expected), IfoodOrderStatus.fromCode(code));
    }

    @Test
    void shouldNotRecognizeObsoleteRcaCode() {
        // RCA nunca existiu na documentação oficial — o código correto é CAR.
        assertThrows(IllegalArgumentException.class, () -> IfoodOrderStatus.fromCode("RCA"));
    }

    @Test
    void shouldThrowForUnknownCode() {
        assertThrows(IllegalArgumentException.class, () -> IfoodOrderStatus.fromCode("XYZ"));
    }

    @Test
    void shouldThrowIllegalArgumentForNullCode() {
        assertThrows(IllegalArgumentException.class, () -> IfoodOrderStatus.fromCode(null));
    }

    @Test
    void shouldMapPlacedToBiteflowAnalise() {
        assertTrue(IfoodOrderStatus.PLACED.toBiteflowStatus().isPresent());
        assertEquals("ANALISE", IfoodOrderStatus.PLACED.toBiteflowStatus().get());
    }

    @Test
    void shouldMapConfirmedToBiteflowPreparo() {
        assertTrue(IfoodOrderStatus.CONFIRMED.toBiteflowStatus().isPresent());
        assertEquals("PREPARO", IfoodOrderStatus.CONFIRMED.toBiteflowStatus().get());
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
        assertTrue(IfoodOrderStatus.READY_TO_PICKUP.toBiteflowStatus().isEmpty());
        assertTrue(IfoodOrderStatus.SEPARATION_STARTED.toBiteflowStatus().isEmpty());
        assertTrue(IfoodOrderStatus.SEPARATION_ENDED.toBiteflowStatus().isEmpty());
        assertTrue(IfoodOrderStatus.CANCELLATION_REQUESTED.toBiteflowStatus().isEmpty());
        assertTrue(IfoodOrderStatus.CANCELLATION_REQUEST_FAILED.toBiteflowStatus().isEmpty());
    }

    @Test
    void onlyOrderLifecycleEventsShouldChangeBiteflowStatus() {
        Set<IfoodOrderStatus> mapped = Set.of(
                IfoodOrderStatus.PLACED, IfoodOrderStatus.CONFIRMED,
                IfoodOrderStatus.DISPATCHED, IfoodOrderStatus.CONCLUDED,
                IfoodOrderStatus.CANCELLED);
        for (IfoodOrderStatus status : IfoodOrderStatus.values()) {
            assertEquals(mapped.contains(status), status.toBiteflowStatus().isPresent(),
                    "Mapeamento inesperado para " + status);
        }
    }
}
