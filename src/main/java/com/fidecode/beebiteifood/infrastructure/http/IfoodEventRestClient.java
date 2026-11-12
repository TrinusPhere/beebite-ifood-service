package com.fidecode.beebiteifood.infrastructure.http;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fidecode.beebiteifood.application.port.IfoodEventClient;
import com.fidecode.beebiteifood.domain.event.entity.IfoodEvent;
import com.fidecode.beebiteifood.domain.shared.exception.IfoodAuthenticationException;
import com.fidecode.beebiteifood.domain.shared.exception.IfoodIntegrationException;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Implementação HTTP do IfoodEventClient.
 * Polling: GET /events/v1.0/events:polling (header x-polling-merchants filtra pelo merchant da loja)
 * ACK:     POST /events/v1.0/events/acknowledgment
 */
@ApplicationScoped
public class IfoodEventRestClient implements IfoodEventClient {

    private static final String POLLING_PATH = "/events/v1.0/events:polling";
    private static final String ACK_PATH = "/events/v1.0/events/acknowledgment";
    private static final String POLLING_MERCHANTS_HEADER = "x-polling-merchants";
    private static final int MAX_ACK_BATCH_SIZE = 2000;

    @ConfigProperty(name = "biteflow.ifood.base-url")
    String baseUrl;

    @Inject
    ObjectMapper objectMapper;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Override
    public List<IfoodEvent> pollEvents(String accessToken, String merchantId) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + POLLING_PATH))
                .header("Authorization", "Bearer " + accessToken);

        // Sem este header o iFood devolve eventos de todos os merchants do token (multi-loja).
        if (merchantId != null && !merchantId.isBlank()) {
            builder.header(POLLING_MERCHANTS_HEADER, merchantId);
        }

        HttpRequest request = builder
                .GET()
                .timeout(Duration.ofSeconds(15))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            // 204 = sem eventos
            if (response.statusCode() == 204) return Collections.emptyList();

            // 401 = access token rejeitado (expirado/revogado): o caso de uso renova e tenta de novo.
            if (response.statusCode() == 401) {
                throw new IfoodAuthenticationException("Polling rejeitado: HTTP 401");
            }

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IfoodIntegrationException("Polling falhou: HTTP " + response.statusCode());
            }

            List<Map<String, Object>> rawEvents = objectMapper.readValue(
                    response.body(), new TypeReference<>() {});

            return rawEvents.stream()
                    .map(this::toIfoodEvent)
                    .toList();

        } catch (IfoodIntegrationException | IfoodAuthenticationException e) {
            throw e;
        } catch (Exception e) {
            throw new IfoodIntegrationException("Erro ao fazer polling de eventos", e);
        }
    }

    @Override
    public void acknowledgeEvents(String accessToken, List<String> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) return;

        // Documentação iFood: ACK suporta no máximo 2000 IDs por requisição.
        for (int i = 0; i < eventIds.size(); i += MAX_ACK_BATCH_SIZE) {
            List<String> batch = eventIds.subList(i, Math.min(i + MAX_ACK_BATCH_SIZE, eventIds.size()));
            sendAckBatch(accessToken, batch);
        }
    }

    private void sendAckBatch(String accessToken, List<String> batch) {
        try {
            List<Map<String, String>> ackBody = batch.stream()
                    .map(id -> Map.of("id", id))
                    .toList();
            String bodyJson = objectMapper.writeValueAsString(ackBody);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + ACK_PATH))
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(bodyJson))
                    .timeout(Duration.ofSeconds(15))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IfoodIntegrationException("ACK falhou: HTTP " + response.statusCode());
            }

        } catch (IfoodIntegrationException e) {
            throw e;
        } catch (Exception e) {
            throw new IfoodIntegrationException("Erro ao enviar ACK ao iFood", e);
        }
    }

    @SuppressWarnings("unchecked")
    private IfoodEvent toIfoodEvent(Map<String, Object> raw) {
        return new IfoodEvent(
                (String) raw.get("id"),
                (String) raw.get("code"),
                (String) raw.get("fullCode"),
                (String) raw.get("orderId"),
                (String) raw.get("merchantId"),
                raw.get("createdAt") != null ? Instant.parse((String) raw.get("createdAt")) : Instant.now(),
                (String) raw.get("salesChannel"),
                raw.get("metadata") instanceof Map ? (Map<String, Object>) raw.get("metadata") : Collections.emptyMap()
        );
    }
}
