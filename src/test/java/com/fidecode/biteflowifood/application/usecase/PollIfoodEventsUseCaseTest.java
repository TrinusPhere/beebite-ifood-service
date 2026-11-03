package com.fidecode.biteflowifood.application.usecase;

import com.fidecode.biteflowifood.application.dto.*;
import com.fidecode.biteflowifood.application.port.IfoodAuthClient;
import com.fidecode.biteflowifood.application.port.IfoodEventClient;
import com.fidecode.biteflowifood.application.port.IfoodOrderClient;
import com.fidecode.biteflowifood.application.port.OrderEventPublisher;
import com.fidecode.biteflowifood.application.port.SecretEncryptor;
import com.fidecode.biteflowifood.domain.credential.entity.IfoodCredential;
import com.fidecode.biteflowifood.domain.credential.repository.IfoodCredentialRepository;
import com.fidecode.biteflowifood.domain.event.entity.IfoodEvent;
import com.fidecode.biteflowifood.domain.event.repository.ProcessedEventRepository;
import com.fidecode.biteflowifood.domain.event.valueobject.IfoodOrderStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PollIfoodEventsUseCaseTest {

    @Mock IfoodCredentialRepository credentialRepository;
    @Mock ProcessedEventRepository processedEventRepository;
    @Mock IfoodAuthClient authClient;
    @Mock IfoodEventClient eventClient;
    @Mock IfoodOrderClient orderClient;
    @Mock OrderEventPublisher eventPublisher;
    @Mock SecretEncryptor secretEncryptor;
    @InjectMocks PollIfoodEventsUseCase useCase;

    private final UUID storeId = UUID.randomUUID();

    private IfoodCredential validCredential() {
        IfoodCredential cred = IfoodCredential.reconstitute(
                UUID.randomUUID(), storeId, "cid", "enc-secret", "mid",
                "valid-token", "refresh-token", "auth-code", java.time.LocalDateTime.now().plusHours(1),
                true, java.time.LocalDateTime.now(), java.time.LocalDateTime.now()
        );
        return cred;
    }

    @Test
    void shouldPublishPlacedEventForNewIfoodOrder() {
        IfoodCredential cred = validCredential();
        when(credentialRepository.findAllEnabled()).thenReturn(List.of(cred));

        IfoodEvent placedEvent = new IfoodEvent(
                "evt-001", "PLC", "ORDER_STATUS_CHANGED", "ifood-order-id",
                "mid", Instant.now(), "IFOOD", Map.of()
        );
        when(eventClient.pollEvents("valid-token", "mid")).thenReturn(List.of(placedEvent));
        when(processedEventRepository.exists("evt-001")).thenReturn(false);

        IfoodOrderDetails details = buildFakeOrderDetails("ifood-order-id");
        when(orderClient.getOrderDetails("valid-token", "ifood-order-id")).thenReturn(details);

        useCase.execute();

        verify(eventPublisher).publishOrderPlaced(any(IfoodOrderPlacedEvent.class));
        verify(processedEventRepository).markProcessed("evt-001", storeId);
        verify(eventClient).acknowledgeEvents(eq("valid-token"), anyList());
    }

    @Test
    void shouldPublishStatusChangedForCancelledOrder() {
        IfoodCredential cred = validCredential();
        when(credentialRepository.findAllEnabled()).thenReturn(List.of(cred));

        IfoodEvent cancelEvent = new IfoodEvent(
                "evt-002", "CAN", "ORDER_STATUS_CHANGED", "ifood-order-id",
                "mid", Instant.now(), "IFOOD", Map.of()
        );
        when(eventClient.pollEvents("valid-token", "mid")).thenReturn(List.of(cancelEvent));
        when(processedEventRepository.exists("evt-002")).thenReturn(false);

        useCase.execute();

        verify(eventPublisher).publishOrderStatusChanged(argThat(e ->
                "CANCELADO".equals(e.newBiteflowStatus()) && "ifood-order-id".equals(e.ifoodOrderId())
        ));
        verify(eventClient).acknowledgeEvents(eq("valid-token"), eq(List.of("evt-002")));
    }

    @Test
    void shouldSkipAlreadyProcessedEvents() {
        IfoodCredential cred = validCredential();
        when(credentialRepository.findAllEnabled()).thenReturn(List.of(cred));

        IfoodEvent duplicateEvent = new IfoodEvent(
                "evt-dup", "PLC", "ORDER_STATUS_CHANGED", "order-id",
                "mid", Instant.now(), "IFOOD", Map.of()
        );
        when(eventClient.pollEvents("valid-token", "mid")).thenReturn(List.of(duplicateEvent));
        when(processedEventRepository.exists("evt-dup")).thenReturn(true); // já processado

        useCase.execute();

        verify(orderClient, never()).getOrderDetails(any(), any());
        verify(eventPublisher, never()).publishOrderPlaced(any());
        // Mas ACK é enviado mesmo assim
        verify(eventClient).acknowledgeEvents(eq("valid-token"), eq(List.of("evt-dup")));
    }

    @Test
    void shouldRenewTokenWhenExpiredBeforePolling() {
        IfoodCredential expired = IfoodCredential.reconstitute(
                UUID.randomUUID(), storeId, "cid", "enc-secret", "mid",
                "old-token", "refresh-token", "auth-code", java.time.LocalDateTime.now().minusMinutes(1), // expirado
                true, java.time.LocalDateTime.now(), java.time.LocalDateTime.now()
        );
        when(credentialRepository.findAllEnabled()).thenReturn(List.of(expired));
        // secretEncryptor descriptografa o enc-secret → plain-secret
        when(secretEncryptor.decrypt("enc-secret")).thenReturn("plain-secret");
        when(authClient.refreshToken("cid", "plain-secret", "refresh-token")).thenReturn(
                new IfoodTokenResponse("new-token", "bearer", 3600L, "new-refresh"));
        when(eventClient.pollEvents(anyString(), eq("mid"))).thenReturn(List.of());

        useCase.execute();

        verify(credentialRepository).save(any(IfoodCredential.class));
    }

    @Test
    void shouldDoNothingWhenNoEnabledCredentials() {
        when(credentialRepository.findAllEnabled()).thenReturn(List.of());

        useCase.execute();

        verifyNoInteractions(eventClient, orderClient, eventPublisher);
    }

    @Test
    void shouldIgnoreIntermediaryStatusEvents() {
        IfoodCredential cred = validCredential();
        when(credentialRepository.findAllEnabled()).thenReturn(List.of(cred));

        IfoodEvent cfmEvent = new IfoodEvent(
                "evt-cfm", "CFM", "ORDER_STATUS_CHANGED", "order-id",
                "mid", Instant.now(), "IFOOD", Map.of()
        );
        when(eventClient.pollEvents("valid-token", "mid")).thenReturn(List.of(cfmEvent));
        when(processedEventRepository.exists("evt-cfm")).thenReturn(false);

        useCase.execute();

        verify(eventPublisher, never()).publishOrderStatusChanged(any());
        verify(processedEventRepository).markProcessed("evt-cfm", storeId);
        verify(eventClient).acknowledgeEvents(eq("valid-token"), eq(List.of("evt-cfm")));
    }

    @Test
    void shouldIgnoreAndNotAckEventsFromOtherMerchants() {
        IfoodCredential cred = validCredential();
        when(credentialRepository.findAllEnabled()).thenReturn(List.of(cred));

        IfoodEvent foreignEvent = new IfoodEvent(
                "evt-foreign", "CAN", "CANCELLED", "order-of-other-store",
                "other-merchant", Instant.now(), "IFOOD", Map.of()
        );
        IfoodEvent ownEvent = new IfoodEvent(
                "evt-own", "CAN", "CANCELLED", "my-order",
                "mid", Instant.now(), "IFOOD", Map.of()
        );
        when(eventClient.pollEvents("valid-token", "mid")).thenReturn(List.of(foreignEvent, ownEvent));
        when(processedEventRepository.exists("evt-own")).thenReturn(false);

        useCase.execute();

        // evento de outra loja: não processa, não marca como processado e NÃO confirma (ACK)
        verify(processedEventRepository, never()).exists("evt-foreign");
        verify(processedEventRepository, never()).markProcessed(eq("evt-foreign"), any());
        verify(eventPublisher, times(1)).publishOrderStatusChanged(any());
        verify(eventClient).acknowledgeEvents(eq("valid-token"), eq(List.of("evt-own")));
    }

    @Test
    void shouldKeepProcessingBatchAndAckOnlySuccessfulEventsWhenOneFails() {
        IfoodCredential cred = validCredential();
        when(credentialRepository.findAllEnabled()).thenReturn(List.of(cred));

        IfoodEvent failingPlaced = new IfoodEvent(
                "evt-fail", "PLC", "PLACED", "order-fail",
                "mid", Instant.now(), "IFOOD", Map.of()
        );
        IfoodEvent okCancel = new IfoodEvent(
                "evt-ok", "CAN", "CANCELLED", "order-ok",
                "mid", Instant.now(), "IFOOD", Map.of()
        );
        when(eventClient.pollEvents("valid-token", "mid")).thenReturn(List.of(failingPlaced, okCancel));
        when(processedEventRepository.exists("evt-fail")).thenReturn(false);
        when(processedEventRepository.exists("evt-ok")).thenReturn(false);
        when(orderClient.getOrderDetails("valid-token", "order-fail"))
                .thenThrow(new com.fidecode.biteflowifood.domain.shared.exception.IfoodIntegrationException("boom"));

        useCase.execute();

        // o evento seguinte ainda é processado
        verify(eventPublisher).publishOrderStatusChanged(argThat(e -> "order-ok".equals(e.ifoodOrderId())));
        verify(processedEventRepository).markProcessed("evt-ok", storeId);
        // o que falhou não é marcado como processado e não é confirmado: o iFood reenvia no próximo ciclo
        verify(processedEventRepository, never()).markProcessed(eq("evt-fail"), any());
        verify(eventClient).acknowledgeEvents(eq("valid-token"), eq(List.of("evt-ok")));
    }

    @Test
    void shouldAckDuplicatesEvenWhenAnotherEventInTheBatchFails() {
        IfoodCredential cred = validCredential();
        when(credentialRepository.findAllEnabled()).thenReturn(List.of(cred));

        IfoodEvent duplicate = new IfoodEvent(
                "evt-dup", "CAN", "CANCELLED", "order-dup",
                "mid", Instant.now(), "IFOOD", Map.of()
        );
        IfoodEvent failing = new IfoodEvent(
                "evt-fail", "PLC", "PLACED", "order-fail",
                "mid", Instant.now(), "IFOOD", Map.of()
        );
        when(eventClient.pollEvents("valid-token", "mid")).thenReturn(List.of(duplicate, failing));
        when(processedEventRepository.exists("evt-dup")).thenReturn(true);
        when(processedEventRepository.exists("evt-fail")).thenReturn(false);
        when(orderClient.getOrderDetails("valid-token", "order-fail"))
                .thenThrow(new RuntimeException("boom"));

        useCase.execute();

        verify(eventClient).acknowledgeEvents(eq("valid-token"), eq(List.of("evt-dup")));
    }

    @Test
    void shouldProcessEventsInChronologicalOrderByCreatedAt() {
        IfoodCredential cred = validCredential();
        when(credentialRepository.findAllEnabled()).thenReturn(List.of(cred));

        Instant t0 = Instant.parse("2026-10-21T10:00:00Z");
        Instant t1 = Instant.parse("2026-10-21T10:05:00Z");

        // Chegam na ordem inversa (t1 antes de t0)
        IfoodEvent laterEvent = new IfoodEvent(
                "evt-later", "DSP", "DISPATCHED", "order-1",
                "mid", t1, "IFOOD", Map.of()
        );
        IfoodEvent earlierEvent = new IfoodEvent(
                "evt-earlier", "CAN", "CANCELLED", "order-1",
                "mid", t0, "IFOOD", Map.of()
        );
        when(eventClient.pollEvents("valid-token", "mid")).thenReturn(List.of(laterEvent, earlierEvent));
        when(processedEventRepository.exists(anyString())).thenReturn(false);

        useCase.execute();

        // O mais antigo (t0) deve ser processado antes do mais recente (t1)
        org.mockito.InOrder inOrder = inOrder(eventPublisher);
        inOrder.verify(eventPublisher).publishOrderStatusChanged(argThat(e -> "CANCELADO".equals(e.newBiteflowStatus())));
        inOrder.verify(eventPublisher).publishOrderStatusChanged(argThat(e -> "EM_ROTA".equals(e.newBiteflowStatus())));
    }

    @Test
    void shouldAcknowledgeAndDiscardUnknownEventCodes() {
        IfoodCredential cred = validCredential();
        when(credentialRepository.findAllEnabled()).thenReturn(List.of(cred));

        IfoodEvent unknownEvent = new IfoodEvent(
                "evt-unknown", "TOTALLY_UNKNOWN_CODE", "SOME_STATUS", "order-1",
                "mid", Instant.now(), "IFOOD", Map.of()
        );
        when(eventClient.pollEvents("valid-token", "mid")).thenReturn(List.of(unknownEvent));
        when(processedEventRepository.exists("evt-unknown")).thenReturn(false);

        useCase.execute();

        verifyNoInteractions(eventPublisher, orderClient);
        verify(processedEventRepository).markProcessed("evt-unknown", storeId);
        verify(eventClient).acknowledgeEvents(eq("valid-token"), eq(List.of("evt-unknown")));
    }

    @Test
    void shouldPublishDisputeReceivedWhenHsdEventReceived() {
        IfoodCredential cred = validCredential();
        when(credentialRepository.findAllEnabled()).thenReturn(List.of(cred));

        Map<String, Object> meta = Map.of(
                "disputeId", "disp-456",
                "handshakeType", "AFTER_DELIVERY",
                "expiresAt", "2026-10-22T10:00:00Z",
                "timeoutAction", "ACCEPT_CANCELLATION"
        );
        IfoodEvent hsdEvent = new IfoodEvent(
                "evt-hsd", "HSD", "HANDSHAKE_DISPUTE", "order-123",
                "mid", Instant.now(), "IFOOD", meta
        );
        when(eventClient.pollEvents("valid-token", "mid")).thenReturn(List.of(hsdEvent));
        when(processedEventRepository.exists("evt-hsd")).thenReturn(false);

        useCase.execute();

        verify(eventPublisher).publishDisputeReceived(argThat(e ->
                "disp-456".equals(e.disputeId()) &&
                "order-123".equals(e.orderId()) &&
                "AFTER_DELIVERY".equals(e.handshakeType()) &&
                storeId.equals(e.storeId())
        ));
        verify(processedEventRepository).markProcessed("evt-hsd", storeId);
        verify(eventClient).acknowledgeEvents(eq("valid-token"), eq(List.of("evt-hsd")));
    }

    @Test
    void shouldPublishDisputeSettledWhenHssEventReceived() {
        IfoodCredential cred = validCredential();
        when(credentialRepository.findAllEnabled()).thenReturn(List.of(cred));

        Map<String, Object> meta = Map.of(
                "disputeId", "disp-456",
                "status", "ACCEPTED"
        );
        IfoodEvent hssEvent = new IfoodEvent(
                "evt-hss", "HSS", "HANDSHAKE_SETTLEMENT", "order-123",
                "mid", Instant.now(), "IFOOD", meta
        );
        when(eventClient.pollEvents("valid-token", "mid")).thenReturn(List.of(hssEvent));
        when(processedEventRepository.exists("evt-hss")).thenReturn(false);

        useCase.execute();

        verify(eventPublisher).publishDisputeSettled(argThat(e ->
                "disp-456".equals(e.disputeId()) &&
                "ACCEPTED".equals(e.status()) &&
                storeId.equals(e.storeId())
        ));
        verify(processedEventRepository).markProcessed("evt-hss", storeId);
        verify(eventClient).acknowledgeEvents(eq("valid-token"), eq(List.of("evt-hss")));
    }

    @Test
    void shouldPublishCancellationRequestedWhenCarEventReceived() {
        IfoodCredential cred = validCredential();
        when(credentialRepository.findAllEnabled()).thenReturn(List.of(cred));

        Map<String, Object> meta = Map.of(
                "reason_code", "501",
                "details", "Cliente ligou desistindo"
        );
        IfoodEvent carEvent = new IfoodEvent(
                "evt-car", "CAR", "CANCELLATION_REQUESTED", "order-123",
                "mid", Instant.now(), "IFOOD", meta
        );
        when(eventClient.pollEvents("valid-token", "mid")).thenReturn(List.of(carEvent));
        when(processedEventRepository.exists("evt-car")).thenReturn(false);

        useCase.execute();

        verify(eventPublisher).publishCancellationRequested(argThat(e ->
                "501".equals(e.reasonCode()) &&
                "Cliente ligou desistindo".equals(e.details()) &&
                "order-123".equals(e.orderId()) &&
                storeId.equals(e.storeId())
        ));
        verify(processedEventRepository).markProcessed("evt-car", storeId);
        verify(eventClient).acknowledgeEvents(eq("valid-token"), eq(List.of("evt-car")));
    }

    private IfoodOrderDetails buildFakeOrderDetails(String orderId) {
        return new IfoodOrderDetails(
                orderId, "N4K7", null, Instant.now().toString(),
                "DELIVERY", "IMMEDIATE",
                new IfoodOrderDetails.IfoodMerchantRef("mid", "Lanchonete X"),
                new IfoodOrderDetails.IfoodCustomer("cust-id", "João Silva", new IfoodOrderDetails.IfoodPhone("+5511999999999", null, null), null, null),
                List.of(new IfoodOrderDetails.IfoodItem("item-id", "X-Burger", 1,
                        BigDecimal.valueOf(25.90), BigDecimal.valueOf(25.90), null, List.of())),
                BigDecimal.valueOf(25.90),  // subTotal
                BigDecimal.TEN,             // totalFee
                List.of(),                  // benefits
                new IfoodOrderDetails.IfoodTotal(BigDecimal.valueOf(35.90), BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.ZERO),
                new IfoodOrderDetails.IfoodPayments(BigDecimal.valueOf(35.90), BigDecimal.ZERO,
                        List.of(new IfoodOrderDetails.IfoodPaymentMethod(
                                BigDecimal.valueOf(35.90), "BRL", "CREDIT", "prepaid", true))),
                new IfoodOrderDetails.IfoodDelivery("IFOOD",
                        new IfoodOrderDetails.IfoodAddress("Rua Teste", "123", "Rua Teste, 123",
                                "Centro", null, "São Paulo", "SP", "BR", "01234-567", null, -23.5, -46.6))
        );
    }
}

