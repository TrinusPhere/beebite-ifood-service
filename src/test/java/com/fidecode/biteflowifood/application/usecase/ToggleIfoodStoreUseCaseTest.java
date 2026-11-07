package com.fidecode.biteflowifood.application.usecase;

import com.fidecode.biteflowifood.application.dto.IfoodInterruptionResponse;
import com.fidecode.biteflowifood.application.dto.ToggleStoreRequest;
import com.fidecode.biteflowifood.application.port.IfoodMerchantClient;
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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ToggleIfoodStoreUseCaseTest {

    @Mock
    IfoodCredentialRepository credentialRepository;
    @Mock
    IfoodMerchantClient merchantClient;
    @Mock
    ProvideValidIfoodTokenUseCase tokenUseCase;

    @InjectMocks
    ToggleIfoodStoreUseCase useCase;

    @Test
    void shouldDeleteActiveInterruptionsWhenActionIsOpen() {
        UUID storeId = UUID.randomUUID();
        IfoodCredential credential = IfoodCredential.reconstitute(
                UUID.randomUUID(), storeId, "clientId", "encSecret", "merchantId",
                "validToken", null, null,
                LocalDateTime.now().plusHours(1), true, null, null
        );

        when(credentialRepository.findByStoreId(storeId)).thenReturn(Optional.of(credential));
        when(tokenUseCase.ensureValid(credential)).thenReturn(credential);
        when(merchantClient.getInterruptions("validToken", "merchantId")).thenReturn(List.of(
                new IfoodInterruptionResponse("int-123", "Fechado", "2023-01-01T10:00:00Z", "2023-01-01T12:00:00Z")
        ));

        ToggleStoreRequest request = new ToggleStoreRequest("OPEN", null);
        useCase.execute(storeId, request);

        verify(merchantClient).deleteInterruption("validToken", "merchantId", "int-123");
        verify(merchantClient, never()).createInterruption(anyString(), anyString(), anyString(), anyString(), anyString());
        verify(tokenUseCase).ensureValid(credential);
    }

    @Test
    void shouldCreateInterruptionWhenActionIsClose() {
        UUID storeId = UUID.randomUUID();
        IfoodCredential credential = IfoodCredential.reconstitute(
                UUID.randomUUID(), storeId, "clientId", "encSecret", "merchantId",
                "validToken", null, null,
                LocalDateTime.now().plusHours(1), true, null, null
        );

        when(credentialRepository.findByStoreId(storeId)).thenReturn(Optional.of(credential));
        when(tokenUseCase.ensureValid(credential)).thenReturn(credential);

        ToggleStoreRequest request = new ToggleStoreRequest("CLOSE", "Motivo X");
        useCase.execute(storeId, request);

        verify(merchantClient).createInterruption(
                eq("validToken"), eq("merchantId"), eq("Motivo X"), anyString(), anyString()
        );
        verify(merchantClient, never()).deleteInterruption(anyString(), anyString(), anyString());
        verify(tokenUseCase).ensureValid(credential);
    }

    @Test
    void shouldThrowExceptionWhenActionIsInvalid() {
        UUID storeId = UUID.randomUUID();
        IfoodCredential credential = IfoodCredential.reconstitute(
                UUID.randomUUID(), storeId, "clientId", "encSecret", "merchantId",
                "validToken", null, null,
                LocalDateTime.now().plusHours(1), true, null, null
        );

        when(credentialRepository.findByStoreId(storeId)).thenReturn(Optional.of(credential));
        when(tokenUseCase.ensureValid(credential)).thenReturn(credential);

        ToggleStoreRequest request = new ToggleStoreRequest("INVALID", null);

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(storeId, request));
    }
}
