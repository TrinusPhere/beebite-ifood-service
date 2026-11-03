package com.fidecode.biteflowifood.application.usecase;

import com.fidecode.biteflowifood.application.dto.BiteflowOrderStatusChangedEvent;
import com.fidecode.biteflowifood.application.dto.IfoodOrderDetails;
import com.fidecode.biteflowifood.application.port.IfoodOrderClient;
import com.fidecode.biteflowifood.application.port.SecretEncryptor;
import com.fidecode.biteflowifood.domain.credential.entity.IfoodCredential;
import com.fidecode.biteflowifood.domain.credential.exception.CredentialNotFoundException;
import com.fidecode.biteflowifood.domain.credential.repository.IfoodCredentialRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SyncStatusToIfoodUseCaseTest {

    @Mock IfoodCredentialRepository credentialRepository;
    @Mock IfoodOrderClient orderClient;
    @Mock SecretEncryptor encryptor;
    @InjectMocks SyncStatusToIfoodUseCase useCase;

    private final UUID storeId = UUID.randomUUID();

    private IfoodCredential validCredential() {
        return IfoodCredential.reconstitute(
                UUID.randomUUID(), storeId, "cid", "enc-secret", "mid",
                "valid-token", "refresh-token", "auth-code", LocalDateTime.now().plusHours(1),
                true, LocalDateTime.now(), LocalDateTime.now()
        );
    }

    private IfoodOrderDetails buildOrderDetails(String orderType, String deliveredBy) {
        return new IfoodOrderDetails(
                "order-123", "N4K7", null, Instant.now().toString(),
                orderType, "IMMEDIATE",
                new IfoodOrderDetails.IfoodMerchantRef("mid", "Restaurante"),
                new IfoodOrderDetails.IfoodCustomer("c1", "Cliente", null, null, null),
                List.of(), BigDecimal.TEN, BigDecimal.ZERO, List.of(),
                new IfoodOrderDetails.IfoodTotal(BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO),
                new IfoodOrderDetails.IfoodPayments(BigDecimal.TEN, BigDecimal.ZERO, List.of()),
                deliveredBy != null ? new IfoodOrderDetails.IfoodDelivery(deliveredBy, null) : null
        );
    }

    @Test
    void shouldConfirmAndStartPreparationOnPreparoStatus() {
        when(credentialRepository.findByStoreId(storeId)).thenReturn(Optional.of(validCredential()));

        BiteflowOrderStatusChangedEvent event = new BiteflowOrderStatusChangedEvent(
                storeId, UUID.randomUUID(), "ifood-order-1", "PREPARO", Instant.now()
        );

        useCase.execute(event);

        verify(orderClient).confirmOrder("valid-token", "ifood-order-1");
        verify(orderClient).startPreparation("valid-token", "ifood-order-1");
        verify(orderClient, never()).readyToPickup(any(), any());
        verify(orderClient, never()).dispatchOrder(any(), any());
    }

    @Test
    void shouldCallReadyToPickupAndDispatchWhenDeliveryByMerchant() {
        when(credentialRepository.findByStoreId(storeId)).thenReturn(Optional.of(validCredential()));
        when(orderClient.getOrderDetails("valid-token", "ifood-order-1"))
                .thenReturn(buildOrderDetails("DELIVERY", "MERCHANT"));

        BiteflowOrderStatusChangedEvent event = new BiteflowOrderStatusChangedEvent(
                storeId, UUID.randomUUID(), "ifood-order-1", "EM_ROTA", Instant.now()
        );

        useCase.execute(event);

        verify(orderClient).readyToPickup("valid-token", "ifood-order-1");
        verify(orderClient).dispatchOrder("valid-token", "ifood-order-1");
    }

    @Test
    void shouldCallReadyToPickupButNeverDispatchWhenTakeout() {
        when(credentialRepository.findByStoreId(storeId)).thenReturn(Optional.of(validCredential()));
        when(orderClient.getOrderDetails("valid-token", "ifood-order-1"))
                .thenReturn(buildOrderDetails("TAKEOUT", null));

        BiteflowOrderStatusChangedEvent event = new BiteflowOrderStatusChangedEvent(
                storeId, UUID.randomUUID(), "ifood-order-1", "EM_ROTA", Instant.now()
        );

        useCase.execute(event);

        verify(orderClient).readyToPickup("valid-token", "ifood-order-1");
        verify(orderClient, never()).dispatchOrder(any(), any());
    }

    @Test
    void shouldCallReadyToPickupButNeverDispatchWhenDineIn() {
        when(credentialRepository.findByStoreId(storeId)).thenReturn(Optional.of(validCredential()));
        when(orderClient.getOrderDetails("valid-token", "ifood-order-1"))
                .thenReturn(buildOrderDetails("DINE_IN", null));

        BiteflowOrderStatusChangedEvent event = new BiteflowOrderStatusChangedEvent(
                storeId, UUID.randomUUID(), "ifood-order-1", "EM_ROTA", Instant.now()
        );

        useCase.execute(event);

        verify(orderClient).readyToPickup("valid-token", "ifood-order-1");
        verify(orderClient, never()).dispatchOrder(any(), any());
    }

    @Test
    void shouldCallReadyToPickupButNeverDispatchWhenDeliveredByIfood() {
        when(credentialRepository.findByStoreId(storeId)).thenReturn(Optional.of(validCredential()));
        when(orderClient.getOrderDetails("valid-token", "ifood-order-1"))
                .thenReturn(buildOrderDetails("DELIVERY", "IFOOD"));

        BiteflowOrderStatusChangedEvent event = new BiteflowOrderStatusChangedEvent(
                storeId, UUID.randomUUID(), "ifood-order-1", "EM_ROTA", Instant.now()
        );

        useCase.execute(event);

        verify(orderClient).readyToPickup("valid-token", "ifood-order-1");
        verify(orderClient, never()).dispatchOrder(any(), any());
    }

    @Test
    void shouldDoNothingWhenCredentialIsDisabled() {
        IfoodCredential disabledCred = IfoodCredential.reconstitute(
                UUID.randomUUID(), storeId, "cid", "enc-secret", "mid",
                "valid-token", "refresh-token", "auth-code", LocalDateTime.now().plusHours(1),
                false, LocalDateTime.now(), LocalDateTime.now()
        );
        when(credentialRepository.findByStoreId(storeId)).thenReturn(Optional.of(disabledCred));

        BiteflowOrderStatusChangedEvent event = new BiteflowOrderStatusChangedEvent(
                storeId, UUID.randomUUID(), "ifood-order-1", "PREPARO", Instant.now()
        );

        useCase.execute(event);

        verifyNoInteractions(orderClient);
    }

    @Test
    void shouldDoNothingWhenTokenIsExpired() {
        IfoodCredential expiredCred = IfoodCredential.reconstitute(
                UUID.randomUUID(), storeId, "cid", "enc-secret", "mid",
                "expired-token", "refresh-token", "auth-code", LocalDateTime.now().minusMinutes(5),
                true, LocalDateTime.now(), LocalDateTime.now()
        );
        when(credentialRepository.findByStoreId(storeId)).thenReturn(Optional.of(expiredCred));

        BiteflowOrderStatusChangedEvent event = new BiteflowOrderStatusChangedEvent(
                storeId, UUID.randomUUID(), "ifood-order-1", "PREPARO", Instant.now()
        );

        useCase.execute(event);

        verifyNoInteractions(orderClient);
    }

    @Test
    void shouldThrowCredentialNotFoundExceptionWhenStoreNotFound() {
        when(credentialRepository.findByStoreId(storeId)).thenReturn(Optional.empty());

        BiteflowOrderStatusChangedEvent event = new BiteflowOrderStatusChangedEvent(
                storeId, UUID.randomUUID(), "ifood-order-1", "PREPARO", Instant.now()
        );

        assertThrows(CredentialNotFoundException.class, () -> useCase.execute(event));
    }
}
