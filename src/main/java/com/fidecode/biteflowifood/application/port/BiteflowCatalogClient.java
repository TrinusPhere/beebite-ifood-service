package com.fidecode.biteflowifood.application.port;

import com.fidecode.biteflowifood.application.dto.biteflow.BiteflowCatalogRequest.*;
import com.fidecode.biteflowifood.application.dto.biteflow.BiteflowCatalogResponse.*;
import java.util.List;
import java.util.UUID;

public interface BiteflowCatalogClient {
    String createCategory(UUID storeId, CreateCategoryRequest request);
    String createProduct(UUID storeId, CreateProductRequest request);
    String createModifierGroup(UUID storeId, CreateModifierGroupRequest request);
    
    List<CategoryResponse> listCategories(UUID storeId);
    ProductResponse getProduct(UUID storeId, UUID productId);
    List<ProductResponse> listProducts(UUID storeId, String categoryId);
    List<ModifierGroupResponse> listModifierGroups(UUID storeId);
}
