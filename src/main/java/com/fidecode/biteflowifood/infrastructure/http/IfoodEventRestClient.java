package com.fidecode.biteflowifood.infrastructure.http;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fidecode.biteflowifood.application.port.IfoodEventClient;
import com.fidecode.biteflowifood.domain.event.entity.IfoodEvent;
import com.fidecode.biteflowifood.domain.shared.exception.IfoodIntegrationException;

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
 * Polling: GET /events/v1.0/events:polling
 * ACK:     POST /events/v1.0/events/acknowledgment
 */
@ApplicationScoped
public class IfoodEventRestClient implements IfoodEventClient {

    private static final String POLLING_PATH = "/events/v1.0/events:polling";
    private static final String ACK_PATH = "/events/v1.0/events/acknowledgment";

    @ConfigProperty(name = "biteflow.ifood.base-url")
    String baseUrl;

    @Inject
    ObjectMapper objectMapper;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Override
    public List<IfoodEvent> pollEvents(String accessToken, String merchantId) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + POLLING_PATH))
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .timeout(Duration.ofSeconds(15))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            // 204 = sem eventos
            if (response.statusCode() == 204) return Collections.emptyList();

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IfoodIntegrationException("Polling falhou: HTTP " + response.statusCode());
            }

            List<Map<String, Object>> rawEvents = objectMapper.readValue(
                    response.body(), new TypeReference<>() {});

            return rawEvents.stream()
                    .map(this::toIfoodEvent)
                    .toList();

        } catch (IfoodIntegrationException e) {
            throw e;
        } catch (Exception e) {
            throw new IfoodIntegrationException("Erro ao fazer polling de eventos", e);
        }
    }

    @Override
    public void acknowledgeEvents(String accessToken, List<String> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) return;

        try {
            List<Map<String, String>> ackBody = eventIds.stream()
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
