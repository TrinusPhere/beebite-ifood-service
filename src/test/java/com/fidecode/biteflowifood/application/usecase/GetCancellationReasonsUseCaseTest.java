package com.fidecode.biteflowifood.application.usecase;

import com.fidecode.biteflowifood.application.dto.IfoodCancellationReason;
import com.fidecode.biteflowifood.application.port.IfoodOrderClient;
import com.fidecode.biteflowifood.domain.credential.entity.IfoodCredential;
import com.fidecode.biteflowifood.domain.credential.exception.CredentialNotFoundException;
import com.fidecode.biteflowifood.domain.credential.repository.IfoodCredentialRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GetCancellationReasonsUseCaseTest {

    @Mock IfoodCredentialRepository credentialRepository;
    @Mock IfoodOrderClient orderClient;
    @InjectMocks GetCancellationReasonsUseCase useCase;

    private final UUID storeId = UUID.randomUUID();

    private IfoodCredential validCredential() {
        return IfoodCredential.reconstitute(
                UUID.randomUUID(), storeId, "cid", "enc-secret", "mid",
                "valid-token", "refresh-token", "auth-code", LocalDateTime.now().plusHours(1),
                true, LocalDateTime.now(), LocalDateTime.now()
        );
    }

    @Test
    void shouldReturnCancellationReasonsWhenCredentialIsValid() {
        when(credentialRepository.findByStoreId(storeId)).thenReturn(Optional.of(validCredential()));
        when(orderClient.getCancellationReasons("valid-token", "order-123"))
                .thenReturn(List.of(
                        new IfoodCancellationReason("501", "PROBLEMAS DE SISTEMA"),
                        new IfoodCancellationReason("503", "ITEM INDISPONÍVEL")
                ));

        List<IfoodCancellationReason> reasons = useCase.execute(storeId, "order-123");

        assertEquals(2, reasons.size());
        assertEquals("501", reasons.get(0).cancelCodeId());
        assertEquals("PROBLEMAS DE SISTEMA", reasons.get(0).description());
    }

    @Test
    void shouldReturnEmptyListWhenCredentialIsDisabled() {
        IfoodCredential disabled = IfoodCredential.reconstitute(
                UUID.randomUUID(), storeId, "cid", "enc-secret", "mid",
                "valid-token", "refresh-token", "auth-code", LocalDateTime.now().plusHours(1),
                false, LocalDateTime.now(), LocalDateTime.now()
        );
        when(credentialRepository.findByStoreId(storeId)).thenReturn(Optional.of(disabled));

        List<IfoodCancellationReason> reasons = useCase.execute(storeId, "order-123");

        assertTrue(reasons.isEmpty());
        verifyNoInteractions(orderClient);
    }

    @Test
    void shouldReturnEmptyListWhenTokenIsExpired() {
        IfoodCredential expired = IfoodCredential.reconstitute(
                UUID.randomUUID(), storeId, "cid", "enc-secret", "mid",
                "old-token", "refresh-token", "auth-code", LocalDateTime.now().minusMinutes(5),
                true, LocalDateTime.now(), LocalDateTime.now()
        );
        when(credentialRepository.findByStoreId(storeId)).thenReturn(Optional.of(expired));

        List<IfoodCancellationReason> reasons = useCase.execute(storeId, "order-123");

        assertTrue(reasons.isEmpty());
        verifyNoInteractions(orderClient);
    }

    @Test
    void shouldThrowCredentialNotFoundExceptionWhenStoreNotFound() {
        when(credentialRepository.findByStoreId(storeId)).thenReturn(Optional.empty());

        assertThrows(CredentialNotFoundException.class, () -> useCase.execute(storeId, "order-123"));
    }
}
