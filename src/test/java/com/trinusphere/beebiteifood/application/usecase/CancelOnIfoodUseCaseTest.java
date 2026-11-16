package com.trinusphere.beebiteifood.application.usecase;

import com.trinusphere.beebiteifood.application.dto.BiteflowOrderCancelledEvent;
import com.trinusphere.beebiteifood.application.port.IfoodOrderClient;
import com.trinusphere.beebiteifood.domain.credential.entity.IfoodCredential;
import com.trinusphere.beebiteifood.domain.credential.repository.IfoodCredentialRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CancelOnIfoodUseCaseTest {

    @Mock IfoodCredentialRepository credentialRepository;
    @Mock IfoodOrderClient orderClient;
    @Mock ProvideValidIfoodTokenUseCase tokenUseCase;
    @InjectMocks CancelOnIfoodUseCase useCase;

    private final UUID storeId = UUID.randomUUID();

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        lenient().when(tokenUseCase.ensureValid(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private IfoodCredential credential(boolean enabled, LocalDateTime tokenExpiresAt) {
        return IfoodCredential.reconstitute(
                UUID.randomUUID(), storeId, "cid", "enc-secret", "mid",
                "valid-token", "refresh-token", "auth-code", tokenExpiresAt,
                enabled, LocalDateTime.now(), LocalDateTime.now());
    }

    private BiteflowOrderCancelledEvent event() {
        return new BiteflowOrderCancelledEvent(storeId, UUID.randomUUID(), "ifood-order-id",
                "503", "Item indisponível", Instant.now());
    }

    @Test
    void shouldForwardCancellationCodeAndReasonToIfood() {
        when(credentialRepository.findByStoreId(storeId))
                .thenReturn(Optional.of(credential(true, LocalDateTime.now().plusHours(1))));

        useCase.execute(event());

        verify(orderClient).requestCancellation("valid-token", "ifood-order-id", "503", "Item indisponível");
    }

    @Test
    void shouldNotCallIfoodWhenIntegrationIsDisabled() {
        when(credentialRepository.findByStoreId(storeId))
                .thenReturn(Optional.of(credential(false, LocalDateTime.now().plusHours(1))));

        useCase.execute(event());

        verify(orderClient, never()).requestCancellation(any(), any(), any(), any());
    }
}
