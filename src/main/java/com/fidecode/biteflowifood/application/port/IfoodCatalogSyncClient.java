package com.fidecode.biteflowifood.application.port;

import com.fidecode.biteflowifood.application.dto.catalog.IfoodCatalogRequest.*;

public interface IfoodCatalogSyncClient {
    String createCategory(String token, String merchantId, IfoodCreateCategoryRequest request);
    String createItem(String token, String merchantId, IfoodCreateItemRequest request);
    void updateItemPrice(String token, String merchantId, String itemId, IfoodUpdatePriceRequest request);
    void updateItemStatus(String token, String merchantId, String itemId, IfoodUpdateStatusRequest request);
}
