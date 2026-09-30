package com.fidecode.biteflowifood.application.usecase;

import com.fidecode.biteflowifood.application.dto.biteflow.BiteflowCatalogResponse.*;
import com.fidecode.biteflowifood.application.dto.catalog.IfoodCatalogRequest.*;
import com.fidecode.biteflowifood.application.port.BiteflowCatalogClient;
import com.fidecode.biteflowifood.application.port.IfoodCatalogSyncClient;
import com.fidecode.biteflowifood.domain.catalog.entity.CatalogMapping;
import com.fidecode.biteflowifood.domain.catalog.repository.CatalogMappingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExportCatalogToIfoodUseCaseTest {

    @Mock
    BiteflowCatalogClient biteflowCatalogClient;

    @Mock
    IfoodCatalogSyncClient ifoodCatalogSyncClient;

    @Mock
    CatalogMappingRepository mappingRepository;

    @InjectMocks
    ExportCatalogToIfoodUseCase useCase;

    UUID storeId = UUID.randomUUID();
    String merchantId = "merchant-123";
    String token = "token-123";

    @BeforeEach
    void setup() {
        reset(biteflowCatalogClient, ifoodCatalogSyncClient, mappingRepository);
    }

    @Test
    @DisplayName("Deve exportar categorias e produtos novos para o iFood e salvar o mapeamento")
    void shouldExportNewCategoriesAndProducts() {
        // Arrange
        UUID catId = UUID.randomUUID();
        UUID prodId = UUID.randomUUID();

        CategoryResponse catResponse = new CategoryResponse(catId.toString(), "Lanches", "AVAILABLE", 1, List.of());
        ProductResponse prodResponse = new ProductResponse(
                prodId.toString(), "X-Burger", catId.toString(), "Descrição",
                BigDecimal.valueOf(15.0), null, 10, "AVAILABLE", "PDV-XBURGER", "DEFAULT",
                List.of(), "PRODUCED", true, null, "UN", true, true,
                List.of(), List.of(), List.of());

        // Primeiro, listar catálogos do iFood
        when(ifoodCatalogSyncClient.listCatalogIds(token, merchantId)).thenReturn(List.of("catalog-id-1"));

        when(biteflowCatalogClient.listCategories(storeId)).thenReturn(List.of(catResponse));
        when(biteflowCatalogClient.listModifierGroups(storeId)).thenReturn(List.of());
        when(biteflowCatalogClient.listProducts(storeId, catId.toString())).thenReturn(List.of(prodResponse));

        when(mappingRepository.findByBiteflowIdAndStoreId(catId, storeId)).thenReturn(Optional.empty());
        when(mappingRepository.findByBiteflowIdAndStoreId(prodId, storeId)).thenReturn(Optional.empty());

        when(ifoodCatalogSyncClient.createCategory(eq(token), eq(merchantId), eq("catalog-id-1"), any())).thenReturn("ifood-cat-1");
        when(ifoodCatalogSyncClient.createProduct(eq(token), eq(merchantId), any())).thenReturn("ifood-prod-1");

        // Act
        useCase.execute(storeId, merchantId, token);

        // Assert
        verify(ifoodCatalogSyncClient, times(1)).listCatalogIds(token, merchantId);
        verify(ifoodCatalogSyncClient, times(1)).createCategory(eq(token), eq(merchantId), eq("catalog-id-1"), any(IfoodCreateCategoryRequest.class));
        verify(ifoodCatalogSyncClient, times(1)).createProduct(eq(token), eq(merchantId), any(IfoodCreateProductRequest.class));
        verify(mappingRepository, times(2)).save(any(CatalogMapping.class));
    }
}
