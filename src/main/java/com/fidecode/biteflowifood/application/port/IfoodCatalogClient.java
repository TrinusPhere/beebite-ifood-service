package com.fidecode.biteflowifood.application.port;

import com.fidecode.biteflowifood.application.dto.catalog.IfoodCatalogResponse.*;
import java.util.List;

public interface IfoodCatalogClient {
    List<IfoodCatalog> listCatalogs(String token, String merchantId);
    List<IfoodCategory> listCategories(String token, String merchantId);
    IfoodCategoryItemsResponse listCategoryItems(String token, String merchantId, String categoryId);
    IfoodItemFull getItem(String token, String merchantId, String itemId);
    List<IfoodItemData> listSellableItems(String token, String merchantId);
}
