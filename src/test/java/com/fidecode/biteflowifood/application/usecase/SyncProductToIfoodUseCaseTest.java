package com.fidecode.biteflowifood.application.usecase;

import com.fidecode.biteflowifood.application.dto.BiteflowCatalogChangedEvent;
import com.fidecode.biteflowifood.application.dto.catalog.IfoodCatalogRequest.IfoodUpdateStatusRequest;
import com.fidecode.biteflowifood.application.port.BiteflowCatalogClient;
import com.fidecode.biteflowifood.application.port.IfoodCatalogSyncClient;
import com.fidecode.biteflowifood.domain.catalog.entity.CatalogMapping;
import com.fidecode.biteflowifood.domain.catalog.repository.CatalogMappingRepository;
import com.fidecode.biteflowifood.domain.credential.entity.IfoodCredential;
import com.fidecode.biteflowifood.domain.credential.repository.IfoodCredentialRepository;
import com.fidecode.biteflowifood.domain.shared.exception.IfoodIntegrationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SyncProductToIfoodUseCaseTest {

    @Mock CatalogMappingRepository mappingRepository;
    @Mock BiteflowCatalogClient biteflowCatalogClient;
    @Mock IfoodCatalogSyncClient ifoodCatalogSyncClient;
    @Mock IfoodCredentialRepository credentialRepository;

    SyncProductToIfoodUseCase useCase;

    UUID storeId;
    UUID productId;
    CatalogMapping mapping;

    @BeforeEach
    void setUp() {
        useCase = new SyncProductToIfoodUseCase(mappingRepository, biteflowCatalogClient, ifoodCatalogSyncClient, credentialRepository);
        storeId = UUID.randomUUID();
        productId = UUID.randomUUID();
        mapping = new CatalogMapping(storeId, "ifood-1", productId, "PRODUCT");
        when(mappingRepository.findByStoreId(storeId)).thenReturn(List.of(mapping));
    }

    private IfoodCredential validCredential() {
        IfoodCredential credential = mock(IfoodCredential.class);
        when(credential.isEnabled()).thenReturn(true);
        when(credential.isTokenValid()).thenReturn(true);
        when(credential.getAccessToken()).thenReturn("token");
        when(credential.getMerchantId()).thenReturn("merchant");
        return credential;
    }

    @Test
    void productDeletedPausesItemInIfoodAndRemovesMapping() {
        IfoodCredential credential = validCredential();
        when(credentialRepository.findByStoreId(storeId)).thenReturn(Optional.of(credential));

        useCase.execute(new BiteflowCatalogChangedEvent(storeId, productId, "PRODUCT_DELETED"));

        verify(ifoodCatalogSyncClient).updateProductStatus(eq("token"), eq("merchant"), eq("ifood-1"), any(IfoodUpdateStatusRequest.class));
        verify(mappingRepository).delete(mapping);
    }

    @Test
    void productDeletedRemovesMappingEvenWithoutValidCredential() {
        when(credentialRepository.findByStoreId(storeId)).thenReturn(Optional.empty());

        useCase.execute(new BiteflowCatalogChangedEvent(storeId, productId, "PRODUCT_DELETED"));

        verify(ifoodCatalogSyncClient, never()).updateProductStatus(any(), any(), any(), any());
        verify(mappingRepository).delete(mapping);
    }

    @Test
    void productDeletedRemovesMappingWhenItemAlreadyGoneFromIfood() {
        IfoodCredential credential = validCredential();
        when(credentialRepository.findByStoreId(storeId)).thenReturn(Optional.of(credential));
        doThrow(new IfoodIntegrationException("Erro: HTTP 404"))
                .when(ifoodCatalogSyncClient).updateProductStatus(any(), any(), any(), any());

        useCase.execute(new BiteflowCatalogChangedEvent(storeId, productId, "PRODUCT_DELETED"));

        verify(mappingRepository).delete(mapping);
    }

    @Test
    void productDeletedRemovesMappingEvenWhenIfoodFails() {
        IfoodCredential credential = validCredential();
        when(credentialRepository.findByStoreId(storeId)).thenReturn(Optional.of(credential));
        doThrow(new IfoodIntegrationException("Erro: HTTP 503"))
                .when(ifoodCatalogSyncClient).updateProductStatus(any(), any(), any(), any());

        useCase.execute(new BiteflowCatalogChangedEvent(storeId, productId, "PRODUCT_DELETED"));

        // Subscriber não faz retry de erros de negócio; manter o vínculo geraria um mapeamento órfão
        verify(mappingRepository).delete(mapping);
    }
}
