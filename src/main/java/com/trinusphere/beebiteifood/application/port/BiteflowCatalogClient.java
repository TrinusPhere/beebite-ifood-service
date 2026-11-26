package com.trinusphere.beebiteifood.application.port;

import com.trinusphere.beebiteifood.application.dto.biteflow.BiteflowCatalogRequest.*;
import com.trinusphere.beebiteifood.application.dto.biteflow.BiteflowCatalogResponse.*;
import java.util.List;
import java.util.UUID;

public interface BiteflowCatalogClient {
    StoreCatalogContextResponse getStoreCatalogContext(UUID storeId);

    String createCategory(UUID storeId, CreateCategoryRequest request);
    String createProduct(UUID storeId, CreateProductRequest request);
    void updateProduct(UUID storeId, UUID productId, CreateProductRequest request);
    String createModifierGroup(UUID storeId, CreateModifierGroupRequest request);

    List<CategoryResponse> listCategories(UUID storeId);
    ProductResponse getProduct(UUID storeId, UUID productId);
    List<ProductResponse> listProducts(UUID storeId, String categoryId);
    List<ModifierGroupResponse> listModifierGroups(UUID storeId);

    ProductResponse findProductByExternalCode(UUID storeId, String externalCode);
}
