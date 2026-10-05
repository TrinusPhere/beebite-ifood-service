package com.fidecode.biteflowifood.application.usecase;

import com.fidecode.biteflowifood.application.dto.biteflow.BiteflowCatalogRequest.CreateProductRequest;
import com.fidecode.biteflowifood.application.dto.biteflow.BiteflowCatalogResponse.CategoryResponse;
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
import com.fidecode.biteflowifood.domain.catalog.entity.CatalogMapping;
import com.fidecode.biteflowifood.domain.catalog.repository.CatalogMappingRepository;
import java.util.Optional;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
                bfProductId, "Hamburguer", "cat-1", "Desc", BigDecimal.valueOf(10.0), null,
                null, 0, "AVAILABLE", "EXT-1", "DEFAULT", null, "RESALE", false, null, "UN", true, true, null, null, null
        );

        when(biteflowCatalogClient.getProduct(storeId, UUID.fromString(bfProductId))).thenReturn(bfProduct);

        IfoodCategory category = 
                new IfoodCategory("cat-1", "Category 1", "AVAILABLE", "DEFAULT");
        when(ifoodCatalogClient.listCategories(token, merchantId)).thenReturn(List.of(category));

        IfoodItemData itemData = 
                new IfoodItemData(ifoodItemId, ifoodItemId, "DEFAULT", "cat-1", "AVAILABLE", null, "EXT-1", "prod-1", null);
        IfoodCategoryItemsResponse categoryItemsResponse = 
                new IfoodCategoryItemsResponse("cat-1", List.of(itemData), null, null, null);
        
        when(ifoodCatalogClient.listCategoryItems(token, merchantId, "cat-1")).thenReturn(categoryItemsResponse);

        // Act
        useCase.execute(storeId, merchantId, token, request);

        // Assert
        verify(ifoodCatalogSyncClient).createProduct(eq(token), eq(merchantId), any());
    }

    @Test
    void testImportWithStaleMappingToDeletedProductRecreatesProductInBiteflow() {
        // Arrange
        String ifoodItemId = "ifood-stale";
        UUID deletedBfProductId = UUID.randomUUID();
        UUID bfCategoryId = UUID.randomUUID();
        String newBfProductId = UUID.randomUUID().toString();

        MergeCatalogUseCase.MergeRequest request = new MergeCatalogUseCase.MergeRequest(List.of(
                new MergeCatalogUseCase.MergeDecision(ifoodItemId, null, "IMPORT")
        ));

        when(ifoodCatalogClient.listCategories(token, merchantId))
                .thenReturn(List.of(new IfoodCategory("cat-1", "Category 1", "AVAILABLE", "DEFAULT")));
        IfoodItemData itemData = new IfoodItemData(ifoodItemId, ifoodItemId, "DEFAULT", "cat-1", "AVAILABLE", null, null, "prod-1", null);
        when(ifoodCatalogClient.listCategoryItems(token, merchantId, "cat-1"))
                .thenReturn(new IfoodCategoryItemsResponse("cat-1", List.of(itemData), null, null, null));

        CatalogMapping staleMapping = new CatalogMapping(storeId, ifoodItemId, deletedBfProductId, "PRODUCT");
        when(mappingRepository.findByIfoodIdAndStoreId(ifoodItemId, storeId)).thenReturn(Optional.of(staleMapping));
        when(mappingRepository.findByIfoodIdAndStoreId("cat-1", storeId))
                .thenReturn(Optional.of(new CatalogMapping(storeId, "cat-1", bfCategoryId, "CATEGORY")));

        // Produto foi excluído no PDV -> API retorna 404 -> client retorna null
        when(biteflowCatalogClient.getProduct(storeId, deletedBfProductId)).thenReturn(null);
        when(biteflowCatalogClient.listCategories(storeId))
                .thenReturn(List.of(new CategoryResponse(bfCategoryId.toString(), "Lanches", "AVAILABLE", 0, List.of())));
        when(biteflowCatalogClient.createProduct(eq(storeId), any(CreateProductRequest.class))).thenReturn(newBfProductId);

        // Act
        useCase.execute(storeId, merchantId, token, request);

        // Assert
        verify(mappingRepository).delete(staleMapping);
        verify(biteflowCatalogClient).createProduct(eq(storeId), any(CreateProductRequest.class));
        verify(mappingRepository).save(argThat(m ->
                ifoodItemId.equals(m.getIfoodId()) && UUID.fromString(newBfProductId).equals(m.getBiteflowId())));
        verify(ifoodCatalogSyncClient, never()).createProduct(any(), any(), any());
    }

    @Test
    void testImportWithOrphanCategoryMappingRecreatesCategory() {
        // Arrange
        String ifoodItemId = "ifood-new";
        UUID deletedCategoryId = UUID.randomUUID();
        String newCategoryId = UUID.randomUUID().toString();
        String newBfProductId = UUID.randomUUID().toString();

        MergeCatalogUseCase.MergeRequest request = new MergeCatalogUseCase.MergeRequest(List.of(
                new MergeCatalogUseCase.MergeDecision(ifoodItemId, null, "IMPORT")
        ));

        when(ifoodCatalogClient.listCategories(token, merchantId))
                .thenReturn(List.of(new IfoodCategory("cat-1", "Bebidas", "AVAILABLE", "DEFAULT")));
        IfoodItemData itemData = new IfoodItemData(ifoodItemId, ifoodItemId, "DEFAULT", "cat-1", "AVAILABLE", null, null, "prod-1", null);
        when(ifoodCatalogClient.listCategoryItems(token, merchantId, "cat-1"))
                .thenReturn(new IfoodCategoryItemsResponse("cat-1", List.of(itemData), null, null, null));

        when(mappingRepository.findByIfoodIdAndStoreId(ifoodItemId, storeId)).thenReturn(Optional.empty());
        CatalogMapping orphanCatMapping = new CatalogMapping(storeId, "cat-1", deletedCategoryId, "CATEGORY");
        when(mappingRepository.findByIfoodIdAndStoreId("cat-1", storeId)).thenReturn(Optional.of(orphanCatMapping));

        // Categoria foi excluída no PDV
        when(biteflowCatalogClient.listCategories(storeId)).thenReturn(List.of());
        when(biteflowCatalogClient.createCategory(eq(storeId), any())).thenReturn(newCategoryId);
        when(biteflowCatalogClient.createProduct(eq(storeId), any(CreateProductRequest.class))).thenReturn(newBfProductId);

        // Act
        useCase.execute(storeId, merchantId, token, request);

        // Assert
        verify(mappingRepository).delete(orphanCatMapping);
        verify(biteflowCatalogClient).createCategory(eq(storeId), argThat(c -> "Bebidas".equals(c.name())));
        verify(biteflowCatalogClient).createProduct(eq(storeId), argThat(p -> newCategoryId.equals(p.categoryId())));
    }

    @Test
    void testLinkOnlyWithDeletedProductThrowsClearError() {
        // Arrange
        String bfProductId = UUID.randomUUID().toString();
        MergeCatalogUseCase.MergeRequest request = new MergeCatalogUseCase.MergeRequest(List.of(
                new MergeCatalogUseCase.MergeDecision("ifood-1", bfProductId, "LINK_ONLY")
        ));
        when(biteflowCatalogClient.getProduct(storeId, UUID.fromString(bfProductId))).thenReturn(null);

        // Act + Assert
        var ex = assertThrows(RuntimeException.class, () -> useCase.execute(storeId, merchantId, token, request));
        assertTrue(ex.getMessage().contains(bfProductId));
        verify(mappingRepository, never()).save(any());
    }

    @Test
    void testLinkOnlyReplacesExistingMappingForSameIfoodItem() {
        // Arrange
        String bfProductId = UUID.randomUUID().toString();
        MergeCatalogUseCase.MergeRequest request = new MergeCatalogUseCase.MergeRequest(List.of(
                new MergeCatalogUseCase.MergeDecision("ifood-1", bfProductId, "LINK_ONLY")
        ));
        when(biteflowCatalogClient.getProduct(storeId, UUID.fromString(bfProductId))).thenReturn(product(bfProductId));
        CatalogMapping oldMapping = new CatalogMapping(storeId, "ifood-1", UUID.randomUUID(), "PRODUCT");
        when(mappingRepository.findByIfoodIdAndStoreId("ifood-1", storeId)).thenReturn(Optional.of(oldMapping));

        // Act
        useCase.execute(storeId, merchantId, token, request);

        // Assert
        verify(mappingRepository).delete(oldMapping);
        verify(mappingRepository).save(argThat(m ->
                "ifood-1".equals(m.getIfoodId()) && UUID.fromString(bfProductId).equals(m.getBiteflowId())));
    }

    private ProductResponse product(String id) {
        return new ProductResponse(id, "Produto", "cat-1", null, BigDecimal.TEN, null,
                null, 0, "AVAILABLE", null, "DEFAULT", null, "RESALE", false, null, "UN", true, true, null, null, null);
    }
}
