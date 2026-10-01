package com.fidecode.biteflowifood.application.usecase;

import com.fidecode.biteflowifood.application.dto.biteflow.BiteflowCatalogRequest.CreateProductRequest;
import com.fidecode.biteflowifood.application.dto.biteflow.BiteflowCatalogResponse.ProductResponse;
import com.fidecode.biteflowifood.application.dto.catalog.IfoodCatalogRequest.IfoodUpdatePriceRequest;
import com.fidecode.biteflowifood.application.dto.catalog.IfoodCatalogRequest.IfoodUpdateProductRequest;
import com.fidecode.biteflowifood.application.dto.catalog.IfoodCatalogRequest.IfoodUpdateStatusRequest;
import com.fidecode.biteflowifood.application.dto.catalog.IfoodCatalogResponse.IfoodCategory;
import com.fidecode.biteflowifood.application.dto.catalog.IfoodCatalogResponse.IfoodCategoryItemsResponse;
import com.fidecode.biteflowifood.application.dto.catalog.IfoodCatalogResponse.IfoodItemData;
import com.fidecode.biteflowifood.application.port.BiteflowCatalogClient;
import com.fidecode.biteflowifood.application.port.IfoodCatalogClient;
import com.fidecode.biteflowifood.application.port.IfoodCatalogSyncClient;
import com.fidecode.biteflowifood.application.port.ImageStoragePort;
import com.fidecode.biteflowifood.domain.catalog.repository.CatalogMappingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MergeCatalogUseCaseTest {

    @Mock
    CatalogMappingRepository mappingRepository;

    @Mock
    BiteflowCatalogClient biteflowCatalogClient;

    @Mock
    IfoodCatalogSyncClient ifoodCatalogSyncClient;

    @Mock
    IfoodCatalogClient ifoodCatalogClient;

    @Mock
    ImageStoragePort imageStoragePort;

    @InjectMocks
    MergeCatalogUseCase useCase;

    private UUID storeId;
    private String merchantId;
    private String token;

    @BeforeEach
    void setUp() {
        storeId = UUID.randomUUID();
        merchantId = "merchant-123";
        token = "token-123";
    }

    @Test
    void testUseBiteflowUpdatesIfoodProductAndPriceAndStatus() {
        // Arrange
        String ifoodItemId = "ifood-1";
        String bfProductId = UUID.randomUUID().toString();

        MergeCatalogUseCase.MergeDecision decision = new MergeCatalogUseCase.MergeDecision(
                ifoodItemId, bfProductId, "USE_BITEFLOW"
        );
        MergeCatalogUseCase.MergeRequest request = new MergeCatalogUseCase.MergeRequest(List.of(decision));

        ProductResponse bfProduct = new ProductResponse(
                bfProductId, "Hamburguer", "cat-1", "Desc", BigDecimal.valueOf(10.0),
                null, 0, "AVAILABLE", "EXT-1", "DEFAULT", null, "RESALE", false, null, "UN", true, true, null, null, null
        );

        when(biteflowCatalogClient.getProduct(storeId, UUID.fromString(bfProductId))).thenReturn(bfProduct);

        IfoodCategory category = 
                new IfoodCategory("cat-1", "Category 1", "AVAILABLE", "DEFAULT");
        when(ifoodCatalogClient.listCategories(token, merchantId)).thenReturn(List.of(category));

        IfoodItemData itemData = 
                new IfoodItemData(ifoodItemId, "DEFAULT", "cat-1", "AVAILABLE", null, "EXT-1", "prod-1", null);
        IfoodCategoryItemsResponse categoryItemsResponse = 
                new IfoodCategoryItemsResponse("cat-1", List.of(itemData), null, null, null);
        
        when(ifoodCatalogClient.listCategoryItems(token, merchantId, "cat-1")).thenReturn(categoryItemsResponse);

        // Act
        useCase.execute(storeId, merchantId, token, request);

        // Assert
        verify(ifoodCatalogSyncClient).createProduct(eq(token), eq(merchantId), any());
    }
}
