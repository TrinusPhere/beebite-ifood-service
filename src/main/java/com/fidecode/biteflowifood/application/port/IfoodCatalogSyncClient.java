package com.fidecode.biteflowifood.application.port;

import com.fidecode.biteflowifood.application.dto.catalog.IfoodCatalogRequest.*;

import java.util.List;

public interface IfoodCatalogSyncClient {
    /**
     * Lista os catálogos do merchant para obter o catalogId.
     * GET /catalog/v2.0/merchants/{merchantId}/catalogs
     */
    List<String> listCatalogIds(String token, String merchantId);

    /**
     * Cria uma categoria dentro de um catálogo.
     * POST /catalog/v2.0/merchants/{merchantId}/catalogs/{catalogId}/categories
     */
    String createCategory(String token, String merchantId, String catalogId, IfoodCreateCategoryRequest request);

    /**
     * Cria um produto.
     * POST /catalog/v2.0/merchants/{merchantId}/products
     */
    String createProduct(String token, String merchantId, IfoodCreateProductRequest request);

    /**
     * Atualiza o preço de um produto.
     * PATCH /catalog/v2.0/merchants/{merchantId}/products/{productId}
     */
    void updateProductPrice(String token, String merchantId, String productId, IfoodUpdatePriceRequest request);

    /**
     * Atualiza o status de um produto.
     * PATCH /catalog/v2.0/merchants/{merchantId}/products/{productId}
     */
    void updateProductStatus(String token, String merchantId, String productId, IfoodUpdateStatusRequest request);
}
