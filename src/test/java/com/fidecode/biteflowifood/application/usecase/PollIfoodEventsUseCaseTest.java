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

