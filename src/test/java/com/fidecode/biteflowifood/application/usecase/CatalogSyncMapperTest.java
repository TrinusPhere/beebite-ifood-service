package com.fidecode.biteflowifood.application.usecase;

import com.fidecode.biteflowifood.application.dto.biteflow.BiteflowCatalogRequest.*;
import com.fidecode.biteflowifood.application.dto.catalog.IfoodCatalogResponse.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CatalogSyncMapperTest {

    @Test
    void shouldMapCategoryCorrectly() {
        IfoodCategory ifoodCategory = new IfoodCategory("cat-1", "Lanches", "AVAILABLE", "DEFAULT");

        CreateCategoryRequest request = CatalogSyncMapper.toCreateCategoryRequest(ifoodCategory);

        assertEquals("Lanches", request.name());
        assertEquals("AVAILABLE", request.status());
    }

    @Test
    void shouldMapProductCorrectly() {
        IfoodPrice price = new IfoodPrice(new BigDecimal("25.50"), null);
        IfoodItemData data = new IfoodItemData("item-1", "item-1", "DEFAULT", "cat-1", "AVAILABLE", price, "PDV-123", "prod-1", null);
        IfoodProduct prod = new IfoodProduct("prod-1", "X-Burger", "Pão, carne e queijo", "http://image.png", null);
        IfoodItemFull fullItem = new IfoodItemFull(data, List.of(prod), List.of(), List.of());

        CreateProductRequest request = CatalogSyncMapper.toCreateProductRequest(fullItem, "DEFAULT");

        assertEquals("X-Burger", request.name());
        assertEquals("Pão, carne e queijo", request.description());
        assertEquals("http://image.png", request.imageUrl());
        assertEquals("PDV-123", request.externalCode());
        assertEquals("DEFAULT", request.catalogItemType());
        assertEquals("AVAILABLE", request.status());
        assertNotNull(request.channelPrices());
        assertEquals(1, request.channelPrices().size());
        assertEquals("IFOOD", request.channelPrices().get(0).channel());
        assertEquals(new BigDecimal("25.50"), request.channelPrices().get(0).price());
        assertEquals(new BigDecimal("25.50"), request.price());
    }

    @Test
    void shouldMapModifierGroupCorrectly() {
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        com.fasterxml.jackson.databind.node.ArrayNode optionIds = mapper.createArrayNode();
        optionIds.add("opt-1");
        IfoodOptionGroup group = new IfoodOptionGroup("group-1", "Escolha a bebida", "MIN_MAX", "MAIN", 1, 1, optionIds);
        IfoodOption opt1 = new IfoodOption("opt-1", "prod-2", new IfoodPrice(new BigDecimal("5.00"), null), "AVAILABLE", "PDV-456");
        IfoodProduct prod2 = new IfoodProduct("prod-2", "Coca Cola", null, null, null);
        IfoodItemFull fullItem = new IfoodItemFull(null, List.of(prod2), List.of(group), List.of(opt1));

        CreateModifierGroupRequest request = CatalogSyncMapper.toCreateModifierGroupRequest(group, fullItem);

        assertEquals("Escolha a bebida", request.name());
        assertEquals(1, request.minSelections());
        assertEquals(1, request.maxSelections());
        assertTrue(request.required());
        assertEquals("RADIO", request.type()); // min = 1, max = 1 -> RADIO
        assertEquals("SPECIFICATION", request.semanticType());
        
        assertEquals(1, request.options().size());
        assertEquals("Coca Cola", request.options().get(0).name());
        assertEquals(new BigDecimal("5.00"), request.options().get(0).price());
    }

    @Test
    void shouldMapModifierGroupTypeCorrectly() {
        assertEquals("SPECIFICATION", CatalogSyncMapper.mapModifierType("MIN_MAX"));
        assertEquals("OFFER_UNIT", CatalogSyncMapper.mapModifierType("ADDITIONAL"));
        assertEquals("CUTLERY", CatalogSyncMapper.mapModifierType("CUTLERY"));
    }

    @Test
    void shouldMapProductTypeCorrectly() {
        assertEquals("DEFAULT", CatalogSyncMapper.mapProductType("DEFAULT"));
        assertEquals("PIZZA", CatalogSyncMapper.mapProductType("PIZZA"));
        assertEquals("COMBO", CatalogSyncMapper.mapProductType("COMBO_V2"));
    }
}
