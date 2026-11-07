package com.fidecode.biteflowifood.application.usecase;

import com.fidecode.biteflowifood.application.dto.IfoodTokenResponse;
import com.fidecode.biteflowifood.application.port.IfoodAuthClient;
import com.fidecode.biteflowifood.application.port.SecretEncryptor;
import com.fidecode.biteflowifood.domain.credential.entity.IfoodCredential;
import com.fidecode.biteflowifood.domain.credential.repository.IfoodCredentialRepository;
import com.fidecode.biteflowifood.domain.shared.exception.BusinessRuleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProvideValidIfoodTokenUseCaseTest {

    @Mock
    private IfoodCredentialRepository repository;
    @Mock
    private IfoodAuthClient authClient;
    @Mock
    private SecretEncryptor encryptor;

    private ProvideValidIfoodTokenUseCase useCase;

    private final UUID storeId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        useCase = new ProvideValidIfoodTokenUseCase(repository, authClient, encryptor);
    }

    @Test
    void shouldReturnExistingTokenWhenValid() {
        IfoodCredential cred = IfoodCredential.reconstitute(
                UUID.randomUUID(), storeId, "client-id", "enc-secret", "merchant-id",
                "valid-token", "refresh-token", "code",
                LocalDateTime.now().plusHours(1), true, LocalDateTime.now(), LocalDateTime.now()
        );

        when(repository.findByStoreId(storeId)).thenReturn(Optional.of(cred));

        IfoodCredential result = useCase.execute(storeId);

        assertEquals("valid-token", result.getAccessToken());
        verifyNoInteractions(authClient);
        verify(repository, never()).save(any());
    }

    @Test
    void shouldRenewTokenWhenExpiredUsingRefreshToken() {
        IfoodCredential cred = IfoodCredential.reconstitute(
                UUID.randomUUID(), storeId, "client-id", "enc-secret", "merchant-id",
                "old-token", "refresh-token", "code",
                LocalDateTime.now().minusMinutes(1), true, LocalDateTime.now(), LocalDateTime.now()
        );

        when(repository.findByStoreId(storeId)).thenReturn(Optional.of(cred));
        when(encryptor.decrypt("enc-secret")).thenReturn("plain-secret");
        when(authClient.refreshToken("client-id", "plain-secret", "refresh-token"))
                .thenReturn(new IfoodTokenResponse("new-token", "bearer", 21600L, "new-refresh"));

        IfoodCredential result = useCase.execute(storeId);

        assertEquals("new-token", result.getAccessToken());
        assertEquals("new-refresh", result.getRefreshToken());
        verify(repository).save(cred);
    }

    @Test
    void shouldAuthenticateWhenNoRefreshToken() {
        IfoodCredential cred = IfoodCredential.reconstitute(
                UUID.randomUUID(), storeId, "client-id", "enc-secret", "merchant-id",
                "old-token", null, "code",
                LocalDateTime.now().minusMinutes(1), true, LocalDateTime.now(), LocalDateTime.now()
        );

        when(repository.findByStoreId(storeId)).thenReturn(Optional.of(cred));
        when(encryptor.decrypt("enc-secret")).thenReturn("plain-secret");
        when(authClient.authenticate("client-id", "plain-secret"))
                .thenReturn(new IfoodTokenResponse("new-token", "bearer", 21600L, null));

        IfoodCredential result = useCase.execute(storeId);

        assertEquals("new-token", result.getAccessToken());
        verify(repository).save(cred);
    }

    @Test
    void shouldInvalidateAndForceRenewalOn401() {
        IfoodCredential cred = IfoodCredential.reconstitute(
                UUID.randomUUID(), storeId, "client-id", "enc-secret", "merchant-id",
                "rejected-token", "refresh-token", "code",
                LocalDateTime.now().plusHours(1), true, LocalDateTime.now(), LocalDateTime.now()
        );

        when(encryptor.decrypt("enc-secret")).thenReturn("plain-secret");
        when(authClient.refreshToken("client-id", "plain-secret", "refresh-token"))
                .thenReturn(new IfoodTokenResponse("renewed-token", "bearer", 21600L, "new-refresh"));

        IfoodCredential result = useCase.invalidateAndRenew(cred);

        assertEquals("renewed-token", result.getAccessToken());
        verify(repository).save(cred);
    }

    @Test
    void shouldThrowExceptionWhenStoreNotFound() {
        when(repository.findByStoreId(storeId)).thenReturn(Optional.empty());

        assertThrows(BusinessRuleException.class, () -> useCase.execute(storeId));
    }
}

