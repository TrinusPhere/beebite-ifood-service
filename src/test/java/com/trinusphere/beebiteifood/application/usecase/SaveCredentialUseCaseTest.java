package com.trinusphere.beebiteifood.application.usecase;

import com.trinusphere.beebiteifood.application.dto.CredentialResponse;
import com.trinusphere.beebiteifood.application.dto.IfoodTokenResponse;
import com.trinusphere.beebiteifood.application.dto.SaveCredentialRequest;
import com.trinusphere.beebiteifood.application.port.IfoodAuthClient;
import com.trinusphere.beebiteifood.application.port.SecretEncryptor;
import com.trinusphere.beebiteifood.domain.credential.entity.IfoodCredential;
import com.trinusphere.beebiteifood.domain.credential.repository.IfoodCredentialRepository;
import com.trinusphere.beebiteifood.domain.shared.exception.IfoodAuthenticationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SaveCredentialUseCaseTest {

    @Mock IfoodCredentialRepository repository;
    @Mock IfoodAuthClient authClient;
    @Mock SecretEncryptor encryptor;
    @InjectMocks SaveCredentialUseCase useCase;

    private final UUID storeId = UUID.randomUUID();

    @Test
    void shouldSaveNewCredentialSuccessfully() {
        SaveCredentialRequest request = new SaveCredentialRequest(storeId, "cid", "secret", "mid");
        when(encryptor.encrypt("secret")).thenReturn("encrypted-secret");
        when(authClient.authenticate("cid", "secret"))
                .thenReturn(new IfoodTokenResponse("token-abc", "bearer", 3600L, "refresh-abc"));
        when(repository.findByStoreId(storeId)).thenReturn(Optional.empty());
        when(repository.save(any(IfoodCredential.class))).thenAnswer(inv -> inv.getArgument(0));

        CredentialResponse response = useCase.execute(request);

        assertNotNull(response);
        assertEquals(storeId, response.storeId());
        assertEquals("cid", response.clientId());
        assertEquals("mid", response.merchantId());
        assertTrue(response.enabled());
        assertTrue(response.tokenValid());

        verify(encryptor).encrypt("secret");
        verify(authClient).authenticate("cid", "secret");
        verify(repository).save(any(IfoodCredential.class));
    }

    @Test
    void shouldUpdateExistingCredentialWhenAlreadyExists() {
        IfoodCredential existing = new IfoodCredential(storeId, "old-cid", "old-enc", "old-mid");
        existing.setId(UUID.randomUUID());

        SaveCredentialRequest request = new SaveCredentialRequest(storeId, "new-cid", "new-secret", "new-mid");
        when(repository.findByStoreId(storeId)).thenReturn(Optional.of(existing));
        when(encryptor.encrypt("new-secret")).thenReturn("new-encrypted");
        when(authClient.authenticate("new-cid", "new-secret"))
                .thenReturn(new IfoodTokenResponse("new-token", "bearer", 3600L, "new-refresh"));
        when(repository.save(any(IfoodCredential.class))).thenAnswer(inv -> inv.getArgument(0));

        CredentialResponse response = useCase.execute(request);

        assertEquals("new-cid", response.clientId());
        verify(repository, never()).save(argThat(c -> c.getClientId().equals("old-cid")));
    }

    @Test
    void shouldThrowWhenAuthenticationFails() {
        SaveCredentialRequest request = new SaveCredentialRequest(storeId, "bad-cid", "bad-secret", "mid");
        when(encryptor.encrypt(anyString())).thenReturn("enc");
        when(authClient.authenticate(anyString(), anyString()))
                .thenThrow(new IfoodAuthenticationException("401 Unauthorized"));

        assertThrows(IfoodAuthenticationException.class, () -> useCase.execute(request));
        verify(repository, never()).save(any());
    }
}
