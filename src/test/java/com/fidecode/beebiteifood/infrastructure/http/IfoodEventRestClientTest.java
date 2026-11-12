package com.fidecode.beebiteifood.infrastructure.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fidecode.beebiteifood.domain.event.entity.IfoodEvent;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Teste do IfoodEventRestClient contra um servidor HTTP local (sem rede externa).
 * Contrato: integracao_ifood/Events/polling_de_eventos.md
 */
class IfoodEventRestClientTest {

    private HttpServer server;
    private IfoodEventRestClient client;

    private final AtomicReference<String> requestedPath = new AtomicReference<>();
    private final AtomicReference<String> pollingMerchantsHeader = new AtomicReference<>();
    private final AtomicReference<String> authorizationHeader = new AtomicReference<>();
    private final List<String> receivedPostBodies = new java.util.concurrent.CopyOnWriteArrayList<>();

    private int responseStatus = 200;
    private String responseBody = "[]";

    @BeforeEach
    void setUp() throws Exception {
        receivedPostBodies.clear();
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/", exchange -> {
            requestedPath.set(exchange.getRequestURI().getPath());
            pollingMerchantsHeader.set(exchange.getRequestHeaders().getFirst("x-polling-merchants"));
            authorizationHeader.set(exchange.getRequestHeaders().getFirst("Authorization"));
            byte[] reqBytes = exchange.getRequestBody().readAllBytes();
            if (reqBytes.length > 0) {
                receivedPostBodies.add(new String(reqBytes, StandardCharsets.UTF_8));
            }
            byte[] body = responseBody.getBytes(StandardCharsets.UTF_8);
            if (responseStatus == 204) {
                exchange.sendResponseHeaders(204, -1);
            } else {
                exchange.sendResponseHeaders(responseStatus, body.length);
                exchange.getResponseBody().write(body);
            }
            exchange.close();
        });
        server.start();

        client = new IfoodEventRestClient();
        client.baseUrl = "http://localhost:" + server.getAddress().getPort();
        client.objectMapper = new ObjectMapper();
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void shouldThrowAuthenticationExceptionWhenPollingReturns401() {
        responseStatus = 401;
        responseBody = "{\"error\":{\"code\":\"Unauthorized\"}}";

        assertThrows(com.fidecode.beebiteifood.domain.shared.exception.IfoodAuthenticationException.class,
                () -> client.pollEvents("expired-token", "merchant-abc"));
    }

    @Test
    void shouldSendPollingMerchantsHeaderWithTheStoreMerchantId() {
        client.pollEvents("token-123", "merchant-abc");

        assertEquals("/events/v1.0/events:polling", requestedPath.get());
        assertEquals("merchant-abc", pollingMerchantsHeader.get());
        assertEquals("Bearer token-123", authorizationHeader.get());
    }

    @Test
    void shouldReturnEmptyListOn204() {
        responseStatus = 204;

        List<IfoodEvent> events = client.pollEvents("token", "merchant-abc");

        assertTrue(events.isEmpty());
    }

    @Test
    void shouldParseEventsIncludingMerchantId() {
        responseBody = "[{\"id\":\"e1\",\"code\":\"PLC\",\"fullCode\":\"PLACED\",\"orderId\":\"o1\","
                + "\"merchantId\":\"merchant-abc\",\"createdAt\":\"2026-10-20T10:00:00Z\",\"salesChannel\":\"IFOOD\"}]";

        List<IfoodEvent> events = client.pollEvents("token", "merchant-abc");

        assertEquals(1, events.size());
        assertEquals("e1", events.get(0).getId());
        assertEquals("PLC", events.get(0).getCode());
        assertEquals("merchant-abc", events.get(0).getMerchantId());
    }

    @Test
    void shouldSendAcknowledgmentPostWithEventIds() {
        responseStatus = 202;
        responseBody = "";

        client.acknowledgeEvents("token-123", List.of("e1", "e2"));

        assertEquals("/events/v1.0/events/acknowledgment", requestedPath.get());
        assertEquals("Bearer token-123", authorizationHeader.get());
        assertEquals(1, receivedPostBodies.size());
        assertTrue(receivedPostBodies.get(0).contains("\"id\":\"e1\""));
        assertTrue(receivedPostBodies.get(0).contains("\"id\":\"e2\""));
    }

    @Test
    void shouldSplitAcknowledgmentIntoBatchesOfMax2000Ids() throws Exception {
        responseStatus = 202;
        responseBody = "";

        // 2001 IDs -> deve gerar 2 requisições: 1 de 2000 e 1 de 1
        List<String> ids = java.util.stream.IntStream.range(0, 2001)
                .mapToObj(i -> "evt-" + i)
                .toList();

        client.acknowledgeEvents("token-123", ids);

        assertEquals(2, receivedPostBodies.size());

        List<Map<String, String>> batch1 = client.objectMapper.readValue(
                receivedPostBodies.get(0), new com.fasterxml.jackson.core.type.TypeReference<>() {});
        List<Map<String, String>> batch2 = client.objectMapper.readValue(
                receivedPostBodies.get(1), new com.fasterxml.jackson.core.type.TypeReference<>() {});

        assertEquals(2000, batch1.size());
        assertEquals(1, batch2.size());
        assertEquals("evt-0", batch1.get(0).get("id"));
        assertEquals("evt-1999", batch1.get(1999).get("id"));
        assertEquals("evt-2000", batch2.get(0).get("id"));
    }
}
