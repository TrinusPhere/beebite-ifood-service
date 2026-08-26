package com.fidecode.biteflowifood.infrastructure.http;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fidecode.biteflowifood.application.dto.IfoodCancellationReason;
import com.fidecode.biteflowifood.application.dto.IfoodOrderDetails;
import com.fidecode.biteflowifood.application.port.IfoodOrderClient;
import com.fidecode.biteflowifood.domain.shared.exception.IfoodIntegrationException;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Implementação HTTP do IfoodOrderClient.
 * Faz operações na API de pedidos do iFood:
 * GET /order/v1.0/orders/{id}
 * POST /order/v1.0/orders/{id}/confirm
 * POST /order/v1.0/orders/{id}/startPreparation
 * POST /order/v1.0/orders/{id}/readyToPickup
 * POST /order/v1.0/orders/{id}/dispatch
 * GET /order/v1.0/orders/{id}/cancellationReasons
 * POST /order/v1.0/orders/{id}/requestCancellation
 */
@ApplicationScoped
public class IfoodOrderRestClient implements IfoodOrderClient {

    private static final String ORDERS_PATH = "/order/v1.0/orders/";

    @ConfigProperty(name = "biteflow.ifood.base-url")
    String baseUrl;

    @Inject
    ObjectMapper objectMapper;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Override
    public IfoodOrderDetails getOrderDetails(String accessToken, String orderId) {
        return doGet(accessToken, ORDERS_PATH + orderId, IfoodOrderDetails.class);
    }

    @Override
    public void confirmOrder(String accessToken, String orderId) {
        doPost(accessToken, ORDERS_PATH + orderId + "/confirm", null);
    }

    @Override
    public void startPreparation(String accessToken, String orderId) {
        doPost(accessToken, ORDERS_PATH + orderId + "/startPreparation", null);
    }

    @Override
    public void readyToPickup(String accessToken, String orderId) {
        doPost(accessToken, ORDERS_PATH + orderId + "/readyToPickup", null);
    }

    @Override
    public void dispatchOrder(String accessToken, String orderId) {
        doPost(accessToken, ORDERS_PATH + orderId + "/dispatch", null);
    }

    @Override
    public List<IfoodCancellationReason> getCancellationReasons(String accessToken, String orderId) {
        try {
            HttpRequest request = buildGet(accessToken, ORDERS_PATH + orderId + "/cancellationReasons");
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            checkStatus(response, "getCancellationReasons");
            List<Map<String, Object>> raw = objectMapper.readValue(response.body(), new TypeReference<>() {});
            return raw.stream()
                    .map(r -> new IfoodCancellationReason(
                            (String) r.get("cancelCode"),
                            (String) r.get("description")
                    )).toList();
        } catch (IfoodIntegrationException e) {
            throw e;
        } catch (Exception e) {
            throw new IfoodIntegrationException("Erro ao buscar motivos de cancelamento", e);
        }
    }

    @Override
    public void requestCancellation(String accessToken, String orderId, String cancellationCode) {
        doPost(accessToken, ORDERS_PATH + orderId + "/requestCancellation",
                Map.of("cancellationCode", cancellationCode));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private <T> T doGet(String accessToken, String path, Class<T> responseType) {
        try {
            HttpRequest request = buildGet(accessToken, path);
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            checkStatus(response, "GET " + path);
            return objectMapper.readValue(response.body(), responseType);
        } catch (IfoodIntegrationException e) {
            throw e;
        } catch (Exception e) {
            throw new IfoodIntegrationException("Erro em GET " + path, e);
        }
    }

    private void doPost(String accessToken, String path, Object body) {
        try {
            String bodyJson = body != null ? objectMapper.writeValueAsString(body) : "";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + path))
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(bodyJson))
                    .timeout(Duration.ofSeconds(15))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            checkStatus(response, "POST " + path);
        } catch (IfoodIntegrationException e) {
            throw e;
        } catch (Exception e) {
            throw new IfoodIntegrationException("Erro em POST " + path, e);
        }
    }

    private HttpRequest buildGet(String accessToken, String path) {
        return HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .timeout(Duration.ofSeconds(15))
                .build();
    }

    private void checkStatus(HttpResponse<?> response, String operation) {
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IfoodIntegrationException(
                    operation + " falhou: HTTP " + response.statusCode() + " - " + response.body());
        }
    }
}
