package com.fidecode.beebiteifood.application.port;

import com.fidecode.beebiteifood.application.dto.catalog.IfoodCatalogRequest.*;

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
     * Atualiza o preço de um item individual (Merge Patch).
     * PATCH /catalog/v2.0/merchants/{merchantId}/items/{itemId}
     */
    void updateProductPrice(String token, String merchantId, String itemId, IfoodUpdatePriceRequest request);

    /**
     * Atualiza o status de um item individual (Merge Patch).
     * PATCH /catalog/v2.0/merchants/{merchantId}/items/{itemId}
     */
    void updateProductStatus(String token, String merchantId, String itemId, IfoodUpdateStatusRequest request);

    /**
     * Atualização parcial completa de um item individual via JSON Merge Patch (RFC 7386).
     * PATCH /catalog/v2.0/merchants/{merchantId}/items/{itemId}
     */
    void patchItem(String token, String merchantId, String itemId, IfoodItemPatchRequest request);

    /**
     * Atualiza preços em lote (assíncrono).
     * PATCH /catalog/v2.0/merchants/{merchantId}/products/price
     */
    IfoodBatchResponse updateProductsPriceBatch(String token, String merchantId, List<IfoodBatchPriceItem> items);

    /**
     * Atualiza status em lote (assíncrono).
     * PATCH /catalog/v2.0/merchants/{merchantId}/products/status
     */
    IfoodBatchResponse updateProductsStatusBatch(String token, String merchantId, List<IfoodBatchStatusItem> items);

    /**
     * Acompanha a execução de um lote assíncrono.
     * GET /catalog/v2.0/merchants/{merchantId}/batch/{batchId}
     */
    IfoodBatchStatusResponse getBatchStatus(String token, String merchantId, String batchId);

    /**
     * Atualiza o preço de uma opção/complemento.
     * PATCH /catalog/v2.0/merchants/{merchantId}/options/price
     */
    void updateOptionPrice(String token, String merchantId, IfoodUpdateOptionPriceRequest request);

    /**
     * Atualiza o status de uma opção/complemento.
     * PATCH /catalog/v2.0/merchants/{merchantId}/options/status
     */
    void updateOptionStatus(String token, String merchantId, IfoodUpdateOptionStatusRequest request);

    /**
     * Atualiza o status de um grupo de opções/complementos inteiro.
     * PATCH /catalog/v2.0/merchants/{merchantId}/optionGroups/{optionGroupId}/status
     */
    void updateOptionGroupStatus(String token, String merchantId, String optionGroupId, IfoodUpdateOptionGroupStatusRequest request);
}

