package com.trinusphere.beebiteifood.infrastructure.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trinusphere.beebiteifood.application.dto.catalog.IfoodCatalogResponse.*;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class IfoodCatalogRestClientTest {

    private HttpServer server;
    private IfoodCatalogRestClient client;

    private final List<String> requestedPaths = new ArrayList<>();

    @BeforeEach
    void setUp() throws Exception {
        requestedPaths.clear();
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/", exchange -> {
            String p = exchange.getRequestURI().getPath();
            requestedPaths.add(p);

            String resp = "[]";
            int status = 200;

            if (p.endsWith("/catalogs")) {
                resp = "[{\"catalogId\":\"cat-id-123\",\"context\":[\"DEFAULT\"],\"status\":\"AVAILABLE\"}]";
            } else if (p.contains("/sellableItems")) {
                resp = "[{\"id\":\"item-1\",\"itemId\":\"item-1\",\"status\":\"AVAILABLE\"}]";
            } else if (p.contains("/unsellableItems")) {
                resp = "[{\"id\":\"cat-1\",\"name\":\"Bebidas\",\"status\":\"UNAVAILABLE\",\"items\":[],\"restrictions\":[\"CATEGORY_PAUSED\"]}]";
            }

            byte[] out = resp.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, out.length);
            exchange.getResponseBody().write(out);
            exchange.close();
        });
        server.start();

        client = new IfoodCatalogRestClient();
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
    void listSellableItemsResolvesCatalogIdAndCallsCatalogEndpoint() {
        List<IfoodItemData> items = client.listSellableItems("token-123", "merchant-1");

        assertNotNull(items);
        assertEquals(1, items.size());
        assertEquals("item-1", items.get(0).id());

        assertEquals(2, requestedPaths.size());
        assertEquals("/catalog/v2.0/merchants/merchant-1/catalogs", requestedPaths.get(0));
        assertEquals("/catalog/v2.0/merchants/merchant-1/catalogs/cat-id-123/sellableItems", requestedPaths.get(1));
    }

    @Test
    void listUnsellableItemsResolvesCatalogIdAndCallsCatalogEndpoint() {
        List<IfoodUnsellableCategory> categories = client.listUnsellableItems("token-123", "merchant-1");

        assertNotNull(categories);
        assertEquals(1, categories.size());
        assertEquals("Bebidas", categories.get(0).name());
        assertEquals(List.of("CATEGORY_PAUSED"), categories.get(0).restrictions());

        assertEquals(2, requestedPaths.size());
        assertEquals("/catalog/v2.0/merchants/merchant-1/catalogs", requestedPaths.get(0));
        assertEquals("/catalog/v2.0/merchants/merchant-1/catalogs/cat-id-123/unsellableItems", requestedPaths.get(1));
    }
}
