package com.fidecode.biteflowifood.application.usecase;

import com.fidecode.biteflowifood.application.dto.GetStatusResponse;
import com.fidecode.biteflowifood.application.dto.IfoodMerchantStatusResponse;
import com.fidecode.biteflowifood.application.port.IfoodAuthClient;
import com.fidecode.biteflowifood.application.port.IfoodMerchantClient;
import com.fidecode.biteflowifood.application.port.SecretEncryptor;
import com.fidecode.biteflowifood.domain.credential.entity.IfoodCredential;
import com.fidecode.biteflowifood.domain.credential.exception.CredentialNotFoundException;
import com.fidecode.biteflowifood.domain.credential.repository.IfoodCredentialRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GetIfoodStatusUseCaseTest {

    @Mock
    IfoodCredentialRepository credentialRepository;
    @Mock
    IfoodMerchantClient merchantClient;
    @Mock
    IfoodAuthClient authClient;
    @Mock
    SecretEncryptor secretEncryptor;

    @InjectMocks
    GetIfoodStatusUseCase useCase;

    @Test
    void shouldReturnOpenStatusWhenAnyOperationIsAvailable() {
        UUID storeId = UUID.randomUUID();
        IfoodCredential credential = IfoodCredential.reconstitute(
                UUID.randomUUID(), storeId, "clientId", "encSecret", "merchantId",
                "validToken", null, null,
                java.time.LocalDateTime.now().plusDays(1), true, null, null
        );

        when(credentialRepository.findByStoreId(storeId)).thenReturn(Optional.of(credential));
        when(merchantClient.getMerchantStatus("validToken", "merchantId")).thenReturn(List.of(
                new IfoodMerchantStatusResponse("DELIVERY", true, "OK"),
                new IfoodMerchantStatusResponse("TAKEOUT", false, "PAUSED")
        ));

        GetStatusResponse response = useCase.execute(storeId);

        assertTrue(response.isOpen());
        assertEquals(2, response.operations().size());
    }

    @Test
    void shouldReturnClosedStatusWhenNoOperationIsAvailable() {
        UUID storeId = UUID.randomUUID();
        IfoodCredential credential = IfoodCredential.reconstitute(
                UUID.randomUUID(), storeId, "clientId", "encSecret", "merchantId",
                "validToken", null, null,
                java.time.LocalDateTime.now().plusDays(1), true, null, null
        );

        when(credentialRepository.findByStoreId(storeId)).thenReturn(Optional.of(credential));
        when(merchantClient.getMerchantStatus("validToken", "merchantId")).thenReturn(List.of(
                new IfoodMerchantStatusResponse("DELIVERY", false, "PAUSED")
        ));

        GetStatusResponse response = useCase.execute(storeId);

        assertFalse(response.isOpen());
        assertEquals(1, response.operations().size());
    }

    @Test
    void shouldThrowExceptionWhenCredentialNotFound() {
        UUID storeId = UUID.randomUUID();
        when(credentialRepository.findByStoreId(storeId)).thenReturn(Optional.empty());

        assertThrows(CredentialNotFoundException.class, () -> useCase.execute(storeId));
    }
}
