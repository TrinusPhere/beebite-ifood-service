package com.trinusphere.beebiteifood.application.usecase;

import com.trinusphere.beebiteifood.application.dto.BiteflowCatalogChangedEvent;
import com.trinusphere.beebiteifood.application.dto.biteflow.BiteflowCatalogResponse.ChannelPriceDto;
import com.trinusphere.beebiteifood.application.dto.biteflow.BiteflowCatalogResponse.ProductResponse;
import com.trinusphere.beebiteifood.application.dto.catalog.IfoodCatalogRequest.IfoodUpdatePriceRequest;
import com.trinusphere.beebiteifood.application.dto.catalog.IfoodCatalogRequest.IfoodUpdateStatusRequest;
import com.trinusphere.beebiteifood.application.port.BiteflowCatalogClient;
import com.trinusphere.beebiteifood.application.port.IfoodCatalogSyncClient;
import com.trinusphere.beebiteifood.domain.catalog.entity.CatalogMapping;
import com.trinusphere.beebiteifood.domain.catalog.repository.CatalogMappingRepository;
import com.trinusphere.beebiteifood.domain.credential.entity.IfoodCredential;
import com.trinusphere.beebiteifood.domain.credential.repository.IfoodCredentialRepository;
import com.trinusphere.beebiteifood.domain.shared.exception.IfoodIntegrationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

    UUID catalogId;
    UUID deliveryStoreId;
    UUID balcaoEdgeStoreId;
    UUID productId;
    CatalogMapping mapping;

    @BeforeEach
    void setUp() {
        useCase = new SyncProductToIfoodUseCase(mappingRepository, biteflowCatalogClient, ifoodCatalogSyncClient, credentialRepository);
        catalogId = UUID.randomUUID();
        deliveryStoreId = UUID.randomUUID();
        balcaoEdgeStoreId = UUID.randomUUID();
        productId = UUID.randomUUID();
        mapping = new CatalogMapping(catalogId, deliveryStoreId, "ifood-1", productId, "PRODUCT");
    }

    private IfoodCredential validCredential() {
        IfoodCredential credential = mock(IfoodCredential.class);
        lenient().when(credential.getStoreId()).thenReturn(deliveryStoreId);
        lenient().when(credential.getCatalogId()).thenReturn(catalogId);
        when(credential.isEnabled()).thenReturn(true);
        when(credential.isTokenValid()).thenReturn(true);
        when(credential.getAccessToken()).thenReturn("token");
        when(credential.getMerchantId()).thenReturn("merchant");
        return credential;
    }

    @Test
    void shouldSyncIfoodChannelPriceAndGlobalStatusWhenProductEditedFromEdgeBalcaoStore() {
        IfoodCredential deliveryCred = validCredential();
        when(mappingRepository.findByBiteflowIdAndCatalogId(productId, catalogId)).thenReturn(Optional.of(mapping));
        when(credentialRepository.findByCatalogId(catalogId)).thenReturn(Optional.of(deliveryCred));

        // Evento emitido quando a loja Balcão (EDGE) atualiza o produto do catálogo compartilhado
        BiteflowCatalogChangedEvent event = new BiteflowCatalogChangedEvent(
                catalogId,
                balcaoEdgeStoreId,
                productId,
                "PRODUCT_UPDATED",
                new BigDecimal("35.90"),
                "AVAILABLE"
        );

        useCase.execute(event);

        ArgumentCaptor<IfoodUpdatePriceRequest> priceCaptor = ArgumentCaptor.forClass(IfoodUpdatePriceRequest.class);
        verify(ifoodCatalogSyncClient).updateProductPrice(eq("token"), eq("merchant"), eq("ifood-1"), priceCaptor.capture());
        assertEquals(0, new BigDecimal("35.90").compareTo(priceCaptor.getValue().price().value()));

        ArgumentCaptor<IfoodUpdateStatusRequest> statusCaptor = ArgumentCaptor.forClass(IfoodUpdateStatusRequest.class);
        verify(ifoodCatalogSyncClient).updateProductStatus(eq("token"), eq("merchant"), eq("ifood-1"), statusCaptor.capture());
        assertEquals("AVAILABLE", statusCaptor.getValue().status());
    }

    @Test
    void shouldFallbackToFetchingProductAndExtractIfoodChannelPriceWhenEventLacksSnapshot() {
        IfoodCredential deliveryCred = validCredential();
        when(mappingRepository.findByStoreId(deliveryStoreId)).thenReturn(List.of(mapping));
        when(credentialRepository.findByStoreId(deliveryStoreId)).thenReturn(Optional.of(deliveryCred));

        ProductResponse product = new ProductResponse(
                productId.toString(), "X-Bacon", "cat-1", null,
                new BigDecimal("28.00"), // preço Balcão
                null, null, 10,
                "UNAVAILABLE", // pausado localmente na loja (effectiveStatus)
                "AVAILABLE",   // disponível globalmente no catálogo (globalStatus)
                "EXT-1", "DEFAULT",
                List.of(new ChannelPriceDto("IFOOD", new BigDecimal("34.90"))),
                "PRODUCED", false, null, "UN", true, true, List.of(), List.of(), List.of()
        );
        when(biteflowCatalogClient.getProduct(deliveryStoreId, productId)).thenReturn(product);

        useCase.execute(new BiteflowCatalogChangedEvent(deliveryStoreId, productId, "PRODUCT_UPDATED"));

        ArgumentCaptor<IfoodUpdatePriceRequest> priceCaptor = ArgumentCaptor.forClass(IfoodUpdatePriceRequest.class);
        verify(ifoodCatalogSyncClient).updateProductPrice(eq("token"), eq("merchant"), eq("ifood-1"), priceCaptor.capture());
        assertEquals(0, new BigDecimal("34.90").compareTo(priceCaptor.getValue().price().value()));

        ArgumentCaptor<IfoodUpdateStatusRequest> statusCaptor = ArgumentCaptor.forClass(IfoodUpdateStatusRequest.class);
        verify(ifoodCatalogSyncClient).updateProductStatus(eq("token"), eq("merchant"), eq("ifood-1"), statusCaptor.capture());
        assertEquals("AVAILABLE", statusCaptor.getValue().status());
    }

    @Test
    void productDeletedPausesItemInIfoodAndRemovesMapping() {
        IfoodCredential credential = validCredential();
        when(mappingRepository.findByBiteflowIdAndCatalogId(productId, catalogId)).thenReturn(Optional.of(mapping));
        when(credentialRepository.findByCatalogId(catalogId)).thenReturn(Optional.of(credential));

        useCase.execute(new BiteflowCatalogChangedEvent(catalogId, balcaoEdgeStoreId, productId, "PRODUCT_DELETED", null, null));

        verify(ifoodCatalogSyncClient).updateProductStatus(eq("token"), eq("merchant"), eq("ifood-1"), any(IfoodUpdateStatusRequest.class));
        verify(mappingRepository).delete(mapping);
    }

    @Test
    void productDeletedRemovesMappingEvenWithoutValidCredential() {
        when(mappingRepository.findByStoreId(deliveryStoreId)).thenReturn(List.of(mapping));
        when(credentialRepository.findByStoreId(deliveryStoreId)).thenReturn(Optional.empty());

        useCase.execute(new BiteflowCatalogChangedEvent(deliveryStoreId, productId, "PRODUCT_DELETED"));

        verify(ifoodCatalogSyncClient, never()).updateProductStatus(any(), any(), any(), any());
        verify(mappingRepository).delete(mapping);
    }

    @Test
    void productDeletedRemovesMappingWhenItemAlreadyGoneFromIfood() {
        IfoodCredential credential = validCredential();
        when(mappingRepository.findByStoreId(deliveryStoreId)).thenReturn(List.of(mapping));
        when(credentialRepository.findByStoreId(deliveryStoreId)).thenReturn(Optional.of(credential));
        doThrow(new IfoodIntegrationException("Erro: HTTP 404"))
                .when(ifoodCatalogSyncClient).updateProductStatus(any(), any(), any(), any());

        useCase.execute(new BiteflowCatalogChangedEvent(deliveryStoreId, productId, "PRODUCT_DELETED"));

        verify(mappingRepository).delete(mapping);
    }

    @Test
    void productDeletedRemovesMappingEvenWhenIfoodFails() {
        IfoodCredential credential = validCredential();
        when(mappingRepository.findByStoreId(deliveryStoreId)).thenReturn(List.of(mapping));
        when(credentialRepository.findByStoreId(deliveryStoreId)).thenReturn(Optional.of(credential));
        doThrow(new IfoodIntegrationException("Erro: HTTP 503"))
                .when(ifoodCatalogSyncClient).updateProductStatus(any(), any(), any(), any());

        useCase.execute(new BiteflowCatalogChangedEvent(deliveryStoreId, productId, "PRODUCT_DELETED"));

        verify(mappingRepository).delete(mapping);
    }
}
