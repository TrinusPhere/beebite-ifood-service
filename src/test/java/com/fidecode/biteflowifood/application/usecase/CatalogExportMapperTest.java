package com.fidecode.biteflowifood.application.usecase;

import com.fidecode.biteflowifood.application.dto.biteflow.BiteflowCatalogResponse.*;
import com.fidecode.biteflowifood.application.dto.catalog.IfoodCatalogRequest.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CatalogExportMapperTest {

    @Test
    void shouldMapCategoryCorrectly() {
        CategoryResponse response = new CategoryResponse("cat-1", "Bebidas", "AVAILABLE", 1, List.of());
        IfoodCreateCategoryRequest request = CatalogExportMapper.toIfoodCategory(response);

        assertEquals("Bebidas", request.name());
        assertEquals("AVAILABLE", request.status());
        assertEquals("DEFAULT", request.template());
    }

    @Test
    void shouldMapProductWithModifiersCorrectly() {
        ProductResponse product = new ProductResponse(
                "prod-1", "Coca Cola", "cat-1", "Gelada", new BigDecimal("5.00"),
                null, null, "AVAILABLE", "EXT-123", "DEFAULT",
                List.of(new ChannelPriceDto("IFOOD", new BigDecimal("6.00"))),
                null, false, null, "UN", true, true, null, null, List.of("group-1")
        );

        ModifierOptionDto option = new ModifierOptionDto("opt-1", "Com gelo", new BigDecimal("0.00"), "prod-gelo-1");
        ModifierGroupResponse group = new ModifierGroupResponse(
                "group-1", "Complementos", "CHECKBOX", false, 0, 1, null, "INGREDIENTS", List.of(option)
        );

        IfoodCreateProductRequest request = CatalogExportMapper.toIfoodProduct(product, List.of(group), "cat-1", "generated-item-1");

        // Validate ItemData
        assertEquals("generated-item-1", request.item().id());
        assertEquals("prod-1", request.item().productId());
        assertEquals("DEFAULT", request.item().type());
        assertEquals("AVAILABLE", request.item().status());
        assertEquals("EXT-123", request.item().externalCode());
        assertEquals(new BigDecimal("6.00"), request.item().price().value());

        // Validate Products array (root product + modifier products)
        assertEquals(2, request.products().size());
        assertEquals("prod-gelo-1", request.products().get(0).id());
        assertEquals("Com gelo", request.products().get(0).name());
        assertEquals("prod-1", request.products().get(1).id());

        // Validate OptionGroups
        assertEquals(1, request.optionGroups().size());
        IfoodOptionGroup outGroup = request.optionGroups().get(0);
        assertEquals("Complementos", outGroup.name());
        assertEquals("INGREDIENTS", outGroup.optionGroupType());
        assertEquals(0, outGroup.min());
        assertEquals(1, outGroup.max());
        // Validate Options
        assertEquals(1, request.options().size());
        IfoodOption outOption = request.options().get(0);
        assertNotNull(outOption.id());
        
        assertEquals(1, outGroup.optionIds().size());
        assertEquals(outOption.id(), outGroup.optionIds().get(0));

        assertEquals("prod-gelo-1", outOption.productId());
        assertEquals("AVAILABLE", outOption.status());
        assertEquals(new BigDecimal("0.00"), outOption.price().value());
    }

    @Test
    void shouldHandleNullPricesAndDefaults() {
        ProductResponse product = new ProductResponse(
                "prod-2", "Agua", "cat-1", null, null,
                null, null, null, null, null,
                null, null, false, null, "UN", true, true, null, null, null
        );

        IfoodCreateProductRequest request = CatalogExportMapper.toIfoodProduct(product, null, "cat-1", "generated-item-2");

        assertEquals("AVAILABLE", request.item().status()); // Default fallback
        assertEquals("DEFAULT", request.item().type()); // Default fallback
        assertEquals(new BigDecimal("0"), request.item().price().value()); // Null price fallback

        assertEquals(1, request.products().size());
        assertEquals(0, request.optionGroups().size());
        assertEquals(0, request.options().size());
    }
}
