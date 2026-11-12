package com.fidecode.beebiteifood.application.port;

import com.fidecode.beebiteifood.application.dto.biteflow.BiteflowCatalogRequest.*;
import com.fidecode.beebiteifood.application.dto.biteflow.BiteflowCatalogResponse.*;
import java.util.List;
import java.util.UUID;

public interface BiteflowCatalogClient {
    String createCategory(UUID storeId, CreateCategoryRequest request);
    String createProduct(UUID storeId, CreateProductRequest request);
    void updateProduct(UUID storeId, UUID productId, CreateProductRequest request);
    String createModifierGroup(UUID storeId, CreateModifierGroupRequest request);

    List<CategoryResponse> listCategories(UUID storeId);
    ProductResponse getProduct(UUID storeId, UUID productId);
    List<ProductResponse> listProducts(UUID storeId, String categoryId);
    List<ModifierGroupResponse> listModifierGroups(UUID storeId);

    /** Retorna o produto com o externalCode informado, ou null se não existir. */
    ProductResponse findProductByExternalCode(UUID storeId, String externalCode);
}
