package com.trinusphere.beebiteifood.infrastructure.http;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trinusphere.beebiteifood.application.dto.IfoodAcceptDisputeRequest;
import com.trinusphere.beebiteifood.application.dto.IfoodAlternativeDisputeRequest;
import com.trinusphere.beebiteifood.application.dto.IfoodCancellationReason;
import com.trinusphere.beebiteifood.application.dto.IfoodOrderDetails;
import com.trinusphere.beebiteifood.application.dto.IfoodRejectDisputeRequest;
import com.trinusphere.beebiteifood.application.port.IfoodOrderClient;
import com.trinusphere.beebiteifood.domain.shared.exception.IfoodIntegrationException;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
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

    private static final Logger LOG = Logger.getLogger(IfoodOrderRestClient.class);
    private static final String ORDERS_PATH = "/order/v1.0/orders/";
    private static final String DISPUTES_PATH = "/order/v1.0/disputes/";

    @ConfigProperty(name = "biteflow.ifood.base-url")
    String baseUrl;

    @Inject
    ObjectMapper objectMapper;

    long retryBackoffMs = 500;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Override
    public IfoodOrderDetails getOrderDetails(String accessToken, String orderId) {
        // Documentação oficial do iFood: evento PLACED pode chegar antes da sincronização dos detalhes (404 transitório).
        return doGetWithRetryOn404(accessToken, ORDERS_PATH + orderId, IfoodOrderDetails.class, 3);
    }

    @Override
    public void confirmOrder(String accessToken, String orderId) {
        doPostWithRetry(accessToken, ORDERS_PATH + orderId + "/confirm", null, 3, true);
    }

    @Override
    public void startPreparation(String accessToken, String orderId) {
        doPostWithRetry(accessToken, ORDERS_PATH + orderId + "/startPreparation", null, 3, true);
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

            // 204: nenhuma política de cancelamento encontrada para o pedido
            if (response.statusCode() == 204 || response.body() == null || response.body().isBlank()) {
                return List.of();
            }

            List<Map<String, Object>> raw = objectMapper.readValue(response.body(), new TypeReference<>() {});
            return raw.stream()
                    .map(r -> new IfoodCancellationReason(
                            (String) r.get("cancelCodeId"),
                            (String) r.get("description")
                    )).toList();
        } catch (IfoodIntegrationException e) {
            throw e;
        } catch (Exception e) {
            throw new IfoodIntegrationException("Erro ao buscar motivos de cancelamento", e);
        }
    }

    @Override
    public void requestCancellation(String accessToken, String orderId, String cancellationCode, String reason) {
        Map<String, String> body = new LinkedHashMap<>();
        if (reason != null && !reason.isBlank()) {
            body.put("reason", reason);
        }
        body.put("cancellationCode", cancellationCode);
        doPost(accessToken, ORDERS_PATH + orderId + "/requestCancellation", body);
    }

    @Override
    public void acceptDispute(String accessToken, String disputeId, IfoodAcceptDisputeRequest request) {
        doPost(accessToken, DISPUTES_PATH + disputeId + "/accept", request);
    }

    @Override
    public void rejectDispute(String accessToken, String disputeId, IfoodRejectDisputeRequest request) {
        doPost(accessToken, DISPUTES_PATH + disputeId + "/reject", request);
    }

    @Override
    public void proposeDisputeAlternative(String accessToken, String disputeId, IfoodAlternativeDisputeRequest request) {
        doPost(accessToken, DISPUTES_PATH + disputeId + "/alternative", request);
    }


    // ── Helpers ───────────────────────────────────────────────────────────────

    private <T> T doGet(String accessToken, String path, Class<T> responseType) {
        return doGetWithRetryOn404(accessToken, path, responseType, 1);
    }

    private <T> T doGetWithRetryOn404(String accessToken, String path, Class<T> responseType, int maxAttempts) {
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                HttpRequest request = buildGet(accessToken, path);
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 404 && attempt < maxAttempts) {
                    LOG.warnf("[IFOOD-ORDER] GET %s retornou 404 (tentativa %d/%d). Aguardando %dms...",
                            path, attempt, maxAttempts, retryBackoffMs * attempt);
                    Thread.sleep(retryBackoffMs * attempt);
                    continue;
                }
                checkStatus(response, "GET " + path);
                return objectMapper.readValue(response.body(), responseType);
            } catch (IfoodIntegrationException e) {
                throw e;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IfoodIntegrationException("Interrompido aguardando retry do pedido", e);
            } catch (Exception e) {
                throw new IfoodIntegrationException("Erro em GET " + path, e);
            }
        }
        throw new IfoodIntegrationException("GET " + path + " falhou após retentativas");
    }

    private void doPost(String accessToken, String path, Object body) {
        doPostWithRetry(accessToken, path, body, 1, false);
    }

    private void doPostWithRetry(String accessToken, String path, Object body, int maxAttempts, boolean ignoreConflict) {
        String bodyJson;
        try {
            bodyJson = body != null ? objectMapper.writeValueAsString(body) : "";
        } catch (Exception e) {
            throw new IfoodIntegrationException("Erro ao serializar body de POST " + path, e);
        }

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(baseUrl + path))
                        .header("Authorization", "Bearer " + accessToken)
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(bodyJson))
                        .timeout(Duration.ofSeconds(15))
                        .build();
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                // Idempotência: HTTP 409 Conflict significa que a transição já ocorreu no iFood
                if (ignoreConflict && response.statusCode() == 409) {
                    LOG.warnf("[IFOOD-ORDER] POST %s retornou HTTP 409 Conflict — transição já ocorrida no iFood. Tratado como idempotente.", path);
                    return;
                }

                // Falhas transitórias (5xx ou 429 Rate Limit) devem ser retentadas dentro da janela de SLA
                if ((response.statusCode() >= 500 || response.statusCode() == 429) && attempt < maxAttempts) {
                    long backoff = retryBackoffMs * attempt;
                    LOG.warnf("[IFOOD-ORDER] POST %s retornou HTTP %d (tentativa %d/%d). Aguardando %dms...",
                            path, response.statusCode(), attempt, maxAttempts, backoff);
                    Thread.sleep(backoff);
                    continue;
                }

                checkStatus(response, "POST " + path);
                return;
            } catch (IfoodIntegrationException e) {
                throw e;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IfoodIntegrationException("Interrompido aguardando retry de POST " + path, e);
            } catch (Exception e) {
                if (attempt < maxAttempts) {
                    long backoff = retryBackoffMs * attempt;
                    LOG.warnf("[IFOOD-ORDER] Erro de comunicação em POST %s (tentativa %d/%d): %s. Aguardando %dms...",
                            path, attempt, maxAttempts, e.getMessage(), backoff);
                    try {
                        Thread.sleep(backoff);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new IfoodIntegrationException("Interrompido durante retry de POST " + path, ie);
                    }
                    continue;
                }
                throw new IfoodIntegrationException("Erro em POST " + path, e);
            }
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
