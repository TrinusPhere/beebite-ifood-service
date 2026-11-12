package com.fidecode.beebiteifood.infrastructure.http;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fidecode.beebiteifood.application.dto.IfoodAcceptDisputeRequest;
import com.fidecode.beebiteifood.application.dto.IfoodAlternativeDisputeRequest;
import com.fidecode.beebiteifood.application.dto.IfoodCancellationReason;
import com.fidecode.beebiteifood.application.dto.IfoodRejectDisputeRequest;
import com.fidecode.beebiteifood.domain.shared.exception.IfoodIntegrationException;
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
 * Teste do IfoodOrderRestClient (cancelamento) contra um servidor HTTP local.
 * Contrato conferido na referência oficial da API (Order):
 *  - GET  /orders/{id}/cancellationReasons  -> array de {cancelCodeId, description}
 *  - POST /orders/{id}/requestCancellation  -> body {reason, cancellationCode}, resposta 202
 */
class IfoodOrderRestClientTest {

    private HttpServer server;
    private IfoodOrderRestClient client;

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

        client = new IfoodOrderRestClient();
        client.baseUrl = "http://localhost:" + server.getAddress().getPort();
        client.objectMapper = new ObjectMapper();
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void shouldParseCancellationReasonsFromTopLevelArrayUsingCancelCodeId() {
        responseBody = "[{\"cancelCodeId\":\"501\",\"description\":\"PROBLEMAS DE SISTEMA\"},"
                + "{\"cancelCodeId\":\"503\",\"description\":\"ITEM INDISPONÍVEL\"}]";

        List<IfoodCancellationReason> reasons = client.getCancellationReasons("tok", "order-1");

        assertEquals("GET", method.get());
        assertEquals("/order/v1.0/orders/order-1/cancellationReasons", path.get());
        assertEquals(2, reasons.size());
        assertEquals("501", reasons.get(0).cancelCodeId());
        assertEquals("PROBLEMAS DE SISTEMA", reasons.get(0).description());
        assertEquals("503", reasons.get(1).cancelCodeId());
    }

    @Test
    void shouldReturnEmptyListWhenIfoodAnswers204NoPolicyFound() {
        responseStatus = 204;

        List<IfoodCancellationReason> reasons = client.getCancellationReasons("tok", "order-1");

        assertTrue(reasons.isEmpty());
    }

    @Test
    void shouldSendReasonAndCancellationCodeOnRequestCancellation() throws Exception {
        responseStatus = 202;

        client.requestCancellation("tok", "order-1", "503", "Item indisponível");

        assertEquals("POST", method.get());
        assertEquals("/order/v1.0/orders/order-1/requestCancellation", path.get());
        JsonNode sent = new ObjectMapper().readTree(body.get());
        assertEquals("503", sent.get("cancellationCode").asText());
        assertEquals("Item indisponível", sent.get("reason").asText());
    }

    @Test
    void shouldOmitReasonWhenBlankButStillSendCancellationCode() throws Exception {
        responseStatus = 202;

        client.requestCancellation("tok", "order-1", "503", null);

        JsonNode sent = new ObjectMapper().readTree(body.get());
        assertEquals("503", sent.get("cancellationCode").asText());
        assertFalse(sent.has("reason"));
    }

    @Test
    void shouldThrowIntegrationExceptionWhenCancellationIsRejected() {
        responseStatus = 400;
        responseBody = "{\"error\":{\"code\":\"OrderExceededCancellationDeadline\"}}";

        assertThrows(IfoodIntegrationException.class,
                () -> client.requestCancellation("tok", "order-1", "503", "x"));
    }

    @Test
    void shouldRetryOn404AndSucceedWhenOrderAppears() {
        java.util.concurrent.atomic.AtomicInteger attempts = new java.util.concurrent.atomic.AtomicInteger();
        client.retryBackoffMs = 10;

        server.removeContext("/");
        server.createContext("/", exchange -> {
            int current = attempts.incrementAndGet();
            if (current == 1) {
                byte[] out = "{\"error\":{\"code\":\"OrderNotFound\"}}".getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(404, out.length);
                exchange.getResponseBody().write(out);
            } else {
                byte[] out = "{\"id\":\"order-1\",\"displayId\":\"ABC1\",\"orderType\":\"DELIVERY\"}".getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, out.length);
                exchange.getResponseBody().write(out);
            }
            exchange.close();
        });

        com.fidecode.beebiteifood.application.dto.IfoodOrderDetails details = client.getOrderDetails("tok", "order-1");

        assertNotNull(details);
        assertEquals("order-1", details.id());
        assertEquals(2, attempts.get());
    }

    @Test
    void shouldThrowIntegrationExceptionWhenOrderRemains404AfterRetries() {
        client.retryBackoffMs = 10;
        responseStatus = 404;
        responseBody = "{\"error\":{\"code\":\"OrderNotFound\"}}";

        assertThrows(IfoodIntegrationException.class, () -> client.getOrderDetails("tok", "order-1"));
    }

    @Test
    void shouldSendAcceptDisputePost() throws Exception {
        responseStatus = 201;

        client.acceptDispute("tok-1", "disp-123", new IfoodAcceptDisputeRequest("REASON_ABC", "Detalhe da aceitação"));

        assertEquals("POST", method.get());
        assertEquals("/order/v1.0/disputes/disp-123/accept", path.get());
        JsonNode sent = new ObjectMapper().readTree(body.get());
        assertEquals("REASON_ABC", sent.get("reason").asText());
        assertEquals("Detalhe da aceitação", sent.get("detailReason").asText());
    }

    @Test
    void shouldSendRejectDisputePost() throws Exception {
        responseStatus = 200;

        client.rejectDispute("tok-1", "disp-123", new IfoodRejectDisputeRequest("HIGH_STORE_DEMAND"));

        assertEquals("POST", method.get());
        assertEquals("/order/v1.0/disputes/disp-123/reject", path.get());
        JsonNode sent = new ObjectMapper().readTree(body.get());
        assertEquals("HIGH_STORE_DEMAND", sent.get("reason").asText());
    }

    @Test
    void shouldSendProposeAlternativePost() throws Exception {
        responseStatus = 200;

        Map<String, Object> meta = Map.of("additionalTimeInMinutes", 15, "reason", "LATE_DELIVERY");
        client.proposeDisputeAlternative("tok-1", "disp-123", new IfoodAlternativeDisputeRequest("ADDITIONAL_TIME", meta));

        assertEquals("POST", method.get());
        assertEquals("/order/v1.0/disputes/disp-123/alternative", path.get());
        JsonNode sent = new ObjectMapper().readTree(body.get());
        assertEquals("ADDITIONAL_TIME", sent.get("type").asText());
        assertEquals(15, sent.get("metadata").get("additionalTimeInMinutes").asInt());
    }

    @Test
    void shouldRetryConfirmOrderOn5xxAndSucceed() {
        java.util.concurrent.atomic.AtomicInteger attempts = new java.util.concurrent.atomic.AtomicInteger();
        client.retryBackoffMs = 10;

        server.removeContext("/");
        server.createContext("/", exchange -> {
            int current = attempts.incrementAndGet();
            if (current == 1) {
                exchange.sendResponseHeaders(503, -1);
            } else {
                exchange.sendResponseHeaders(202, -1);
            }
            exchange.close();
        });

        assertDoesNotThrow(() -> client.confirmOrder("tok", "order-123"));
        assertEquals(2, attempts.get());
    }

    @Test
    void shouldRetryConfirmOrderOn429TooManyRequests() {
        java.util.concurrent.atomic.AtomicInteger attempts = new java.util.concurrent.atomic.AtomicInteger();
        client.retryBackoffMs = 10;

        server.removeContext("/");
        server.createContext("/", exchange -> {
            int current = attempts.incrementAndGet();
            if (current == 1) {
                exchange.sendResponseHeaders(429, -1);
            } else {
                exchange.sendResponseHeaders(202, -1);
            }
            exchange.close();
        });

        assertDoesNotThrow(() -> client.confirmOrder("tok", "order-123"));
        assertEquals(2, attempts.get());
    }

    @Test
    void shouldTreat409ConflictAsIdempotentOnConfirmOrder() {
        responseStatus = 409;
        responseBody = "{\"error\":{\"code\":\"OrderAlreadyConfirmed\"}}";

        assertDoesNotThrow(() -> client.confirmOrder("tok", "order-123"));
    }

    @Test
    void shouldThrowIntegrationExceptionWhenConfirmOrderFailsAfterRetries() {
        client.retryBackoffMs = 10;
        responseStatus = 500;
        responseBody = "{\"error\":{\"code\":\"InternalServerError\"}}";

        assertThrows(IfoodIntegrationException.class, () -> client.confirmOrder("tok", "order-123"));
    }
}
