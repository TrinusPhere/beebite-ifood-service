package com.trinusphere.beebiteifood.application.usecase;

import com.trinusphere.beebiteifood.application.dto.CredentialResponse;
import com.trinusphere.beebiteifood.application.dto.IfoodTokenResponse;
import com.trinusphere.beebiteifood.application.dto.SaveCredentialRequest;
import com.trinusphere.beebiteifood.application.dto.biteflow.BiteflowCatalogResponse.StoreCatalogContextResponse;
import com.trinusphere.beebiteifood.application.port.BiteflowCatalogClient;
import com.trinusphere.beebiteifood.application.port.IfoodAuthClient;
import com.trinusphere.beebiteifood.application.port.SecretEncryptor;
import com.trinusphere.beebiteifood.domain.credential.entity.IfoodCredential;
import com.trinusphere.beebiteifood.domain.credential.repository.IfoodCredentialRepository;
import com.trinusphere.beebiteifood.domain.shared.exception.BusinessRuleException;
import com.trinusphere.beebiteifood.domain.shared.exception.IfoodAuthenticationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
    @Mock BiteflowCatalogClient biteflowCatalogClient;

    SaveCredentialUseCase useCase;

    private final UUID storeId = UUID.randomUUID();
    private final UUID tenantId = UUID.randomUUID();
    private final UUID catalogId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        useCase = new SaveCredentialUseCase(repository, authClient, encryptor, biteflowCatalogClient);
    }

    @Test
    void shouldSaveNewCredentialSuccessfullyWithCatalogIdFromCloudStore() {
        SaveCredentialRequest request = new SaveCredentialRequest(storeId, "cid", "secret", "mid");
        when(biteflowCatalogClient.getStoreCatalogContext(storeId))
                .thenReturn(new StoreCatalogContextResponse(storeId, tenantId, catalogId, "Cardápio Principal", "CLOUD", true));
        when(repository.findByMerchantId("mid")).thenReturn(Optional.empty());
        when(repository.findByCatalogId(catalogId)).thenReturn(Optional.empty());
        when(encryptor.encrypt("secret")).thenReturn("encrypted-secret");
        when(authClient.authenticate("cid", "secret"))
                .thenReturn(new IfoodTokenResponse("token-abc", "bearer", 3600L, "refresh-abc"));
        when(repository.findByStoreId(storeId)).thenReturn(Optional.empty());
        when(repository.save(any(IfoodCredential.class))).thenAnswer(inv -> inv.getArgument(0));

        CredentialResponse response = useCase.execute(request);

        assertNotNull(response);
        assertEquals(storeId, response.storeId());
        assertEquals(catalogId, response.catalogId());
        assertEquals("cid", response.clientId());
        assertEquals("mid", response.merchantId());
        assertTrue(response.enabled());
        assertTrue(response.tokenValid());

        verify(repository).save(argThat(c -> catalogId.equals(c.getCatalogId())));
    }

    @Test
    void shouldRejectEdgeStoreFromConnectingIfoodDirectly() {
        SaveCredentialRequest request = new SaveCredentialRequest(storeId, "cid", "secret", "mid");
        when(biteflowCatalogClient.getStoreCatalogContext(storeId))
                .thenReturn(new StoreCatalogContextResponse(storeId, tenantId, catalogId, "Cardápio Principal", "EDGE", true));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () -> useCase.execute(request));
        assertTrue(ex.getMessage().contains("EDGE") || ex.getMessage().contains("Cloud"));
        verifyNoInteractions(authClient);
        verify(repository, never()).save(any());
    }

    @Test
    void shouldRejectMerchantIdAlreadyLinkedToAnotherStore() {
        UUID otherStoreId = UUID.randomUUID();
        IfoodCredential existing = new IfoodCredential(otherStoreId, UUID.randomUUID(), "other-cid", "enc", "mid");
        when(biteflowCatalogClient.getStoreCatalogContext(storeId))
                .thenReturn(new StoreCatalogContextResponse(storeId, tenantId, catalogId, "Cardápio Principal", "CLOUD", true));
        when(repository.findByMerchantId("mid")).thenReturn(Optional.of(existing));

        assertThrows(BusinessRuleException.class, () ->
                useCase.execute(new SaveCredentialRequest(storeId, "cid", "secret", "mid")));
        verifyNoInteractions(authClient);
        verify(repository, never()).save(any());
    }

    @Test
    void shouldRejectSecondStoreConnectingToSameSharedCatalog() {
        UUID otherCloudStoreId = UUID.randomUUID();
        IfoodCredential existingCatalogCred = new IfoodCredential(otherCloudStoreId, catalogId, "cid-1", "enc", "mid-1");
        when(biteflowCatalogClient.getStoreCatalogContext(storeId))
                .thenReturn(new StoreCatalogContextResponse(storeId, tenantId, catalogId, "Cardápio Principal", "CLOUD", true));
        when(repository.findByMerchantId("mid-2")).thenReturn(Optional.empty());
        when(repository.findByCatalogId(catalogId)).thenReturn(Optional.of(existingCatalogCred));

        assertThrows(BusinessRuleException.class, () ->
                useCase.execute(new SaveCredentialRequest(storeId, "cid-2", "secret", "mid-2")));
        verifyNoInteractions(authClient);
        verify(repository, never()).save(any());
    }

    @Test
    void shouldUpdateExistingCredentialWhenAlreadyExistsForSameStore() {
        IfoodCredential existing = new IfoodCredential(storeId, catalogId, "old-cid", "old-enc", "old-mid");
        existing.setId(UUID.randomUUID());

        SaveCredentialRequest request = new SaveCredentialRequest(storeId, "new-cid", "new-secret", "new-mid");
        when(biteflowCatalogClient.getStoreCatalogContext(storeId))
                .thenReturn(new StoreCatalogContextResponse(storeId, tenantId, catalogId, "Cardápio Principal", "CLOUD", true));
        when(repository.findByMerchantId("new-mid")).thenReturn(Optional.empty());
        when(repository.findByCatalogId(catalogId)).thenReturn(Optional.of(existing));
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
        when(biteflowCatalogClient.getStoreCatalogContext(storeId))
                .thenReturn(new StoreCatalogContextResponse(storeId, tenantId, catalogId, "Cardápio Principal", "CLOUD", true));
        when(repository.findByMerchantId("mid")).thenReturn(Optional.empty());
        when(repository.findByCatalogId(catalogId)).thenReturn(Optional.empty());
        when(encryptor.encrypt(anyString())).thenReturn("enc");
        when(authClient.authenticate(anyString(), anyString()))
                .thenThrow(new IfoodAuthenticationException("401 Unauthorized"));

        assertThrows(IfoodAuthenticationException.class, () -> useCase.execute(request));
        verify(repository, never()).save(any());
    }
}
