package com.fidecode.biteflowifood.infrastructure.http;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fidecode.biteflowifood.application.dto.IfoodInterruptionResponse;
import com.fidecode.biteflowifood.application.dto.IfoodMerchantStatusResponse;
import com.fidecode.biteflowifood.application.port.IfoodMerchantClient;
import com.fidecode.biteflowifood.domain.shared.exception.IfoodIntegrationException;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class IfoodMerchantRestClient implements IfoodMerchantClient {

    private static final String STATUS_PATH = "/merchant/v1.0/merchants/%s/status";
    private static final String INTERRUPTIONS_PATH = "/merchant/v1.0/merchants/%s/interruptions";
    private static final String INTERRUPTIONS_DELETE_PATH = "/merchant/v1.0/merchants/%s/interruptions/%s";

    @ConfigProperty(name = "biteflow.ifood.base-url")
    String baseUrl;

    @Inject
    ObjectMapper objectMapper;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Override
    public List<IfoodMerchantStatusResponse> getMerchantStatus(String accessToken, String merchantId) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + String.format(STATUS_PATH, merchantId)))
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .timeout(Duration.ofSeconds(15))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return objectMapper.readValue(response.body(), new TypeReference<>() {});
            }
            if (response.statusCode() == 204) return Collections.emptyList();
            throw new IfoodIntegrationException("Erro ao buscar status: HTTP " + response.statusCode());
        } catch (Exception e) {
            throw new IfoodIntegrationException("Erro ao chamar API de Merchant Status", e);
        }
    }

    @Override
    public List<IfoodInterruptionResponse> getInterruptions(String accessToken, String merchantId) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + String.format(INTERRUPTIONS_PATH, merchantId)))
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .timeout(Duration.ofSeconds(15))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return objectMapper.readValue(response.body(), new TypeReference<>() {});
            }
            if (response.statusCode() == 204) return Collections.emptyList();
            throw new IfoodIntegrationException("Erro ao buscar interrupções: HTTP " + response.statusCode());
        } catch (Exception e) {
            throw new IfoodIntegrationException("Erro ao chamar API de Interruptions", e);
        }
    }

    @Override
    public void createInterruption(String accessToken, String merchantId, String description, String start, String end) {
        try {
            Map<String, String> body = Map.of(
                    "description", description,
                    "start", start,
                    "end", end
            );
            String bodyJson = objectMapper.writeValueAsString(body);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + String.format(INTERRUPTIONS_PATH, merchantId)))
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(bodyJson))
                    .timeout(Duration.ofSeconds(15))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IfoodIntegrationException("Erro ao criar interrupção: HTTP " + response.statusCode());
            }
        } catch (Exception e) {
            throw new IfoodIntegrationException("Erro ao chamar API de Interruptions (Create)", e);
        }
    }

    @Override
    public void deleteInterruption(String accessToken, String merchantId, String interruptionId) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + String.format(INTERRUPTIONS_DELETE_PATH, merchantId, interruptionId)))
                .header("Authorization", "Bearer " + accessToken)
                .DELETE()
                .timeout(Duration.ofSeconds(15))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IfoodIntegrationException("Erro ao deletar interrupção: HTTP " + response.statusCode());
            }
        } catch (Exception e) {
            throw new IfoodIntegrationException("Erro ao chamar API de Interruptions (Delete)", e);
        }
    }
}
