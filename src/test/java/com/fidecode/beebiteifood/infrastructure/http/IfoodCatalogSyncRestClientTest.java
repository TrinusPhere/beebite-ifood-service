package com.fidecode.beebiteifood.infrastructure.http;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fidecode.beebiteifood.application.dto.catalog.IfoodCatalogRequest.*;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class IfoodCatalogSyncRestClientTest {

    private HttpServer server;
    private IfoodCatalogSyncRestClient client;

    private final AtomicReference<String> method = new AtomicReference<>();
    private final AtomicReference<String> path = new AtomicReference<>();
    private final AtomicReference<String> body = new AtomicReference<>();

    private int responseStatus = 200;
    private String responseBody = "";

    @BeforeEach
    void setUp() throws Exception {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/", exchange -> {
            method.set(exchange.getRequestMethod());
            path.set(exchange.getRequestURI().getPath());
            body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] out = responseBody.getBytes(StandardCharsets.UTF_8);
            if (out.length == 0) {
                exchange.sendResponseHeaders(responseStatus, -1);
            } else {
                exchange.sendResponseHeaders(responseStatus, out.length);
                exchange.getResponseBody().write(out);
            }
            exchange.close();
        });
        server.start();

        client = new IfoodCatalogSyncRestClient();
        client.baseUrl = "http://localhost:" + server.getAddress().getPort();
        client.objectMapper = new ObjectMapper();
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void updateProductPriceSendsMergePatchPriceObject() throws Exception {
        responseStatus = 200;
        responseBody = "";

        client.updateProductPrice("token-123", "merchant-1", "item-abc",
                new IfoodUpdatePriceRequest(new BigDecimal("29.90"), new BigDecimal("35.00")));

        assertEquals("PATCH", method.get());
        assertEquals("/catalog/v2.0/merchants/merchant-1/items/item-abc", path.get());

        JsonNode json = new ObjectMapper().readTree(body.get());
        assertTrue(json.has("price"));
        assertEquals(29.90, json.get("price").get("value").asDouble(), 0.001);
        assertEquals(35.00, json.get("price").get("originalValue").asDouble(), 0.001);
    }

    @Test
    void updateProductStatusSendsMergePatchStatus() throws Exception {
        responseStatus = 200;
        responseBody = "";

        client.updateProductStatus("token-123", "merchant-1", "item-abc",
                new IfoodUpdateStatusRequest("UNAVAILABLE"));

        assertEquals("PATCH", method.get());
        assertEquals("/catalog/v2.0/merchants/merchant-1/items/item-abc", path.get());

        JsonNode json = new ObjectMapper().readTree(body.get());
        assertTrue(json.has("status"));
        assertEquals("UNAVAILABLE", json.get("status").asText());
    }

    @Test
    void patchItemSendsFullMergePatchPayload() throws Exception {
        responseStatus = 200;
        responseBody = "";

        IfoodItemPatchRequest request = new IfoodItemPatchRequest(
                "AVAILABLE",
                new IfoodPrice(new BigDecimal("19.99"), new BigDecimal("25.00")),
                "PDV-001",
                1,
                List.of(new IfoodShift("08:00", "22:00", true, true, true, true, true, false, false))
        );

        client.patchItem("token-123", "merchant-1", "item-abc", request);

        assertEquals("PATCH", method.get());
        assertEquals("/catalog/v2.0/merchants/merchant-1/items/item-abc", path.get());

        JsonNode json = new ObjectMapper().readTree(body.get());
        assertEquals("AVAILABLE", json.get("status").asText());
        assertEquals("PDV-001", json.get("externalCode").asText());
        assertEquals(1, json.get("index").asInt());
        assertEquals(19.99, json.get("price").get("value").asDouble(), 0.001);
        assertTrue(json.has("shifts"));
        assertEquals(1, json.get("shifts").size());
        assertEquals("08:00", json.get("shifts").get(0).get("startTime").asText());
    }

    @Test
    void updateProductsPriceBatchSendsBulkPayload() throws Exception {
        responseStatus = 200;
        responseBody = "{\"batchId\":\"batch-123\",\"url\":\"/batch/batch-123\"}";

        List<IfoodBatchPriceItem> items = List.of(
                new IfoodBatchPriceItem("prod-1", "EXT-1", new IfoodPrice(BigDecimal.TEN, BigDecimal.TEN), List.of("ITEM"), "cat-1")
        );

        IfoodBatchResponse resp = client.updateProductsPriceBatch("token-123", "merchant-1", items);

        assertEquals("PATCH", method.get());
        assertEquals("/catalog/v2.0/merchants/merchant-1/products/price", path.get());
        assertNotNull(resp);
        assertEquals("batch-123", resp.batchId());

        JsonNode json = new ObjectMapper().readTree(body.get());
        assertTrue(json.isArray());
        assertEquals("prod-1", json.get(0).get("productId").asText());
    }

    @Test
    void updateProductsStatusBatchSendsBulkPayload() throws Exception {
        responseStatus = 200;
        responseBody = "{\"batchId\":\"batch-456\",\"url\":\"/batch/batch-456\"}";

        List<IfoodBatchStatusItem> items = List.of(
                new IfoodBatchStatusItem("prod-1", "EXT-1", "AVAILABLE", List.of("ITEM"), "cat-1")
        );

        IfoodBatchResponse resp = client.updateProductsStatusBatch("token-123", "merchant-1", items);

        assertEquals("PATCH", method.get());
        assertEquals("/catalog/v2.0/merchants/merchant-1/products/status", path.get());
        assertNotNull(resp);
        assertEquals("batch-456", resp.batchId());
    }

    @Test
    void getBatchStatusReturnsStatusAndResults() {
        responseStatus = 200;
        responseBody = "{\"batchId\":\"batch-123\",\"status\":\"COMPLETED\",\"results\":[{\"resource\":\"prod-1\",\"result\":\"SUCCESS\"}]}";

        IfoodBatchStatusResponse resp = client.getBatchStatus("token-123", "merchant-1", "batch-123");

        assertEquals("GET", method.get());
        assertEquals("/catalog/v2.0/merchants/merchant-1/batch/batch-123", path.get());
        assertEquals("batch-123", resp.batchId());
        assertEquals("COMPLETED", resp.status());
        assertEquals(1, resp.results().size());
        assertEquals("SUCCESS", resp.results().get(0).result());
    }

    @Test
    void updateOptionPriceSendsPatch() {
        responseStatus = 200;
        responseBody = "";

        client.updateOptionPrice("token-123", "merchant-1",
                new IfoodUpdateOptionPriceRequest("opt-1", new IfoodPrice(BigDecimal.ONE, BigDecimal.ONE), null, List.of()));

        assertEquals("PATCH", method.get());
        assertEquals("/catalog/v2.0/merchants/merchant-1/options/price", path.get());
    }

    @Test
    void updateOptionStatusSendsPatch() {
        responseStatus = 200;
        responseBody = "";

        client.updateOptionStatus("token-123", "merchant-1",
                new IfoodUpdateOptionStatusRequest("opt-1", "AVAILABLE", null, List.of()));

        assertEquals("PATCH", method.get());
        assertEquals("/catalog/v2.0/merchants/merchant-1/options/status", path.get());
    }

    @Test
    void updateOptionGroupStatusSendsPatch() {
        responseStatus = 200;
        responseBody = "";

        client.updateOptionGroupStatus("token-123", "merchant-1", "group-1",
                new IfoodUpdateOptionGroupStatusRequest("AVAILABLE"));

        assertEquals("PATCH", method.get());
        assertEquals("/catalog/v2.0/merchants/merchant-1/optionGroups/group-1/status", path.get());
    }
}
