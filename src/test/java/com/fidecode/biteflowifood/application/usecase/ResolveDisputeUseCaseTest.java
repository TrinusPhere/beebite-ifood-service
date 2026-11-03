package com.fidecode.biteflowifood.application.usecase;

import com.fidecode.biteflowifood.application.dto.IfoodAcceptDisputeRequest;
import com.fidecode.biteflowifood.application.dto.IfoodAlternativeDisputeRequest;
import com.fidecode.biteflowifood.application.dto.IfoodRejectDisputeRequest;
import com.fidecode.biteflowifood.application.port.IfoodOrderClient;
import com.fidecode.biteflowifood.domain.credential.entity.IfoodCredential;
import com.fidecode.biteflowifood.domain.credential.exception.CredentialNotFoundException;
import com.fidecode.biteflowifood.domain.credential.repository.IfoodCredentialRepository;
import com.fidecode.biteflowifood.domain.shared.exception.IfoodIntegrationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResolveDisputeUseCaseTest {

    @Mock
    IfoodCredentialRepository credentialRepository;

    @Mock
    IfoodOrderClient orderClient;

    @InjectMocks
    ResolveDisputeUseCase useCase;

    private final UUID storeId = UUID.randomUUID();

    private IfoodCredential validCredential() {
        return IfoodCredential.reconstitute(
                UUID.randomUUID(), storeId, "cid", "enc-secret", "mid",
                "valid-token", "refresh-token", "auth-code", LocalDateTime.now().plusHours(1),
                true, LocalDateTime.now(), LocalDateTime.now()
        );
    }

    @Test
    void shouldAcceptDisputeWhenCredentialValid() {
        when(credentialRepository.findByStoreId(storeId)).thenReturn(Optional.of(validCredential()));

        IfoodAcceptDisputeRequest request = new IfoodAcceptDisputeRequest("REASON_1", "Aceito cancelamento");
        useCase.accept(storeId, "disp-1", request);

        verify(orderClient).acceptDispute("valid-token", "disp-1", request);
    }

    @Test
    void shouldRejectDisputeWhenCredentialValid() {
        when(credentialRepository.findByStoreId(storeId)).thenReturn(Optional.of(validCredential()));

        IfoodRejectDisputeRequest request = new IfoodRejectDisputeRequest("HIGH_STORE_DEMAND");
        useCase.reject(storeId, "disp-1", request);

        verify(orderClient).rejectDispute("valid-token", "disp-1", request);
    }

    @Test
    void shouldProposeAlternativeWhenCredentialValid() {
        when(credentialRepository.findByStoreId(storeId)).thenReturn(Optional.of(validCredential()));

        IfoodAlternativeDisputeRequest request = new IfoodAlternativeDisputeRequest(
                "ADDITIONAL_TIME", Map.of("additionalTimeInMinutes", 15)
        );
        useCase.proposeAlternative(storeId, "disp-1", request);

        verify(orderClient).proposeDisputeAlternative("valid-token", "disp-1", request);
    }

    @Test
    void shouldThrowCredentialNotFoundExceptionWhenStoreNotFound() {
        when(credentialRepository.findByStoreId(storeId)).thenReturn(Optional.empty());

        assertThrows(CredentialNotFoundException.class,
                () -> useCase.accept(storeId, "disp-1", new IfoodAcceptDisputeRequest("R", "D")));
    }

    @Test
    void shouldThrowIfoodIntegrationExceptionWhenCredentialDisabled() {
        IfoodCredential disabled = IfoodCredential.reconstitute(
                UUID.randomUUID(), storeId, "cid", "enc-secret", "mid",
                "valid-token", "refresh-token", "auth-code", LocalDateTime.now().plusHours(1),
                false, LocalDateTime.now(), LocalDateTime.now()
        );
        when(credentialRepository.findByStoreId(storeId)).thenReturn(Optional.of(disabled));

        assertThrows(IfoodIntegrationException.class,
                () -> useCase.accept(storeId, "disp-1", new IfoodAcceptDisputeRequest("R", "D")));
        verifyNoInteractions(orderClient);
    }

    @Test
    void shouldThrowIfoodIntegrationExceptionWhenTokenInvalid() {
        IfoodCredential expired = IfoodCredential.reconstitute(
                UUID.randomUUID(), storeId, "cid", "enc-secret", "mid",
                "old-token", "refresh-token", "auth-code", LocalDateTime.now().minusMinutes(5),
                true, LocalDateTime.now(), LocalDateTime.now()
        );
        when(credentialRepository.findByStoreId(storeId)).thenReturn(Optional.of(expired));

        assertThrows(IfoodIntegrationException.class,
                () -> useCase.reject(storeId, "disp-1", new IfoodRejectDisputeRequest("R")));
        verifyNoInteractions(orderClient);
    }
}
