package com.fidecode.biteflowifood.infrastructure.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fidecode.biteflowifood.application.dto.catalog.IfoodCatalogRequest.*;
import com.fidecode.biteflowifood.application.port.IfoodCatalogSyncClient;
import com.fidecode.biteflowifood.domain.shared.exception.IfoodIntegrationException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@ApplicationScoped
public class IfoodCatalogSyncRestClient implements IfoodCatalogSyncClient {

    private static final String CATEGORIES_PATH = "/catalog/v2.0/merchants/%s/categories";
    private static final String ITEMS_PATH = "/catalog/v2.0/merchants/%s/items";
    private static final String ITEM_PRICE_PATH = "/catalog/v2.0/merchants/%s/items/%s/price";
    private static final String ITEM_STATUS_PATH = "/catalog/v2.0/merchants/%s/items/%s/status";

    @ConfigProperty(name = "biteflow.ifood.base-url")
    String baseUrl;

    @Inject
    ObjectMapper objectMapper;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Override
    public String createCategory(String token, String merchantId, IfoodCreateCategoryRequest request) {
        return post(token, String.format(CATEGORIES_PATH, merchantId), request);
    }

    @Override
    public String createItem(String token, String merchantId, IfoodCreateItemRequest request) {
        return post(token, String.format(ITEMS_PATH, merchantId), request);
    }

    @Override
    public void updateItemPrice(String token, String merchantId, String itemId, IfoodUpdatePriceRequest request) {
        patch(token, String.format(ITEM_PRICE_PATH, merchantId, itemId), request);
    }

    @Override
    public void updateItemStatus(String token, String merchantId, String itemId, IfoodUpdateStatusRequest request) {
        patch(token, String.format(ITEM_STATUS_PATH, merchantId, itemId), request);
    }

    private String post(String token, String path, Object body) {
        try {
            String bodyJson = objectMapper.writeValueAsString(body);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + path))
                    .header("Authorization", "Bearer " + token)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(bodyJson))
                    .timeout(Duration.ofSeconds(15))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                // Return id if created
                return extractIdFromResponse(response.body());
            }
            throw new IfoodIntegrationException("Erro ao criar item no iFood: HTTP " + response.statusCode());
        } catch (Exception e) {
            throw new IfoodIntegrationException("Erro ao chamar API do iFood (POST)", e);
        }
    }

    private void patch(String token, String path, Object body) {
        try {
            String bodyJson = objectMapper.writeValueAsString(body);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + path))
                    .header("Authorization", "Bearer " + token)
                    .header("Content-Type", "application/json")
                    .method("PATCH", HttpRequest.BodyPublishers.ofString(bodyJson))
                    .timeout(Duration.ofSeconds(15))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return;
            }
            throw new IfoodIntegrationException("Erro ao atualizar item no iFood: HTTP " + response.statusCode());
        } catch (Exception e) {
            throw new IfoodIntegrationException("Erro ao chamar API do iFood (PATCH)", e);
        }
    }

    private String extractIdFromResponse(String responseBody) {
        try {
            var node = objectMapper.readTree(responseBody);
            if (node.has("id")) {
                return node.get("id").asText();
            }
            return responseBody;
        } catch (Exception e) {
            return responseBody;
        }
    }
}
