package com.trinusphere.beebiteifood.application.usecase;

import com.trinusphere.beebiteifood.application.dto.biteflow.BiteflowCatalogResponse.CategoryResponse;
import com.trinusphere.beebiteifood.application.dto.biteflow.BiteflowCatalogResponse.ChannelPriceDto;
import com.trinusphere.beebiteifood.application.dto.biteflow.BiteflowCatalogResponse.ProductResponse;
import com.trinusphere.beebiteifood.application.dto.catalog.CatalogDiffResponse;
import com.trinusphere.beebiteifood.application.dto.catalog.IfoodCatalogResponse.IfoodCategory;
import com.trinusphere.beebiteifood.application.dto.catalog.IfoodCatalogResponse.IfoodCategoryItemsResponse;
import com.trinusphere.beebiteifood.application.dto.catalog.IfoodCatalogResponse.IfoodItemData;
import com.trinusphere.beebiteifood.application.dto.catalog.IfoodCatalogResponse.IfoodPrice;
import com.trinusphere.beebiteifood.application.dto.catalog.IfoodCatalogResponse.IfoodProduct;
import com.trinusphere.beebiteifood.application.port.BiteflowCatalogClient;
import com.trinusphere.beebiteifood.application.port.IfoodCatalogClient;
import com.trinusphere.beebiteifood.domain.catalog.entity.CatalogMapping;
import com.trinusphere.beebiteifood.domain.catalog.repository.CatalogMappingRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompareCatalogUseCaseTest {

    @Mock IfoodCatalogClient ifoodCatalogClient;
    @Mock BiteflowCatalogClient biteflowCatalogClient;
    @Mock CatalogMappingRepository mappingRepository;

    @InjectMocks CompareCatalogUseCase useCase;

    @Test
    void shouldCompareByCatalogMappingsAndUseGlobalStatusAndIfoodChannelPrice() {
        UUID storeId = UUID.randomUUID();
        UUID catalogId = UUID.randomUUID();
        UUID prodId = UUID.randomUUID();
        String merchantId = "merchant-1";
        String token = "tok-1";

        when(biteflowCatalogClient.listCategories(storeId))
                .thenReturn(List.of(new CategoryResponse("cat-bf", "Lanches", "AVAILABLE", 0, List.of())));

        ProductResponse bfProd = new ProductResponse(
                prodId.toString(), "X-Bacon", "cat-bf", null,
                new BigDecimal("28.00"), // preço Balcão diferente do iFood
                null, null, 5,
                "UNAVAILABLE", // pausado apenas localmente em uma loja (effective status)
                "AVAILABLE",   // disponível globalmente no catálogo (globalStatus == iFood)
                "EXT-1", "DEFAULT",
                List.of(new ChannelPriceDto("IFOOD", new BigDecimal("34.90"))),
                "PRODUCED", false, null, "UN", true, true, List.of(), List.of(), List.of()
        );
        when(biteflowCatalogClient.listProducts(storeId, "cat-bf")).thenReturn(List.of(bfProd));

        when(ifoodCatalogClient.listSellableItems(token, merchantId)).thenReturn(List.of());
        when(ifoodCatalogClient.listCategories(token, merchantId))
                .thenReturn(List.of(new IfoodCategory("cat-if", "Lanches", "AVAILABLE", "DEFAULT")));
        IfoodItemData ifItem = new IfoodItemData(
                "if-item-1", "if-item-1", "DEFAULT", "cat-if", "AVAILABLE",
                new IfoodPrice(new BigDecimal("34.90"), null), "EXT-1", "if-prod-1", null);
        IfoodProduct ifProd = new IfoodProduct("if-prod-1", "X-Bacon", null, null, null);
        when(ifoodCatalogClient.listCategoryItems(token, merchantId, "cat-if"))
                .thenReturn(new IfoodCategoryItemsResponse("cat-if", List.of(ifItem), List.of(ifProd), List.of(), List.of()));

        when(mappingRepository.findByCatalogId(catalogId))
                .thenReturn(List.of(new CatalogMapping(catalogId, storeId, "if-item-1", prodId, "PRODUCT")));

        CatalogDiffResponse diff = useCase.execute(storeId, catalogId, merchantId, token);

        assertEquals(1, diff.totalMatches());
        assertEquals(0, diff.totalConflicts());
    }
}
