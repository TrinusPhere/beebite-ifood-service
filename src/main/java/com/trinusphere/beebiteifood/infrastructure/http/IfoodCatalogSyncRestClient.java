package com.trinusphere.beebiteifood.infrastructure.http;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trinusphere.beebiteifood.application.dto.catalog.IfoodCatalogRequest.*;
import com.trinusphere.beebiteifood.application.port.IfoodCatalogSyncClient;
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
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class IfoodCatalogSyncRestClient implements IfoodCatalogSyncClient {

    private static final Logger LOG = Logger.getLogger(IfoodCatalogSyncRestClient.class);

    // iFood Catalog v2.0 endpoints (corrigidos conforme documentação oficial)
    private static final String CATALOGS_PATH = "/catalog/v2.0/merchants/%s/catalogs";
    private static final String CATEGORIES_PATH = "/catalog/v2.0/merchants/%s/catalogs/%s/categories";
    private static final String ITEMS_PATH = "/catalog/v2.0/merchants/%s/items";
    private static final String ITEM_BY_ID_PATH = "/catalog/v2.0/merchants/%s/items/%s";
    private static final String PRODUCTS_PRICE_PATH = "/catalog/v2.0/merchants/%s/products/price";
    private static final String PRODUCTS_STATUS_PATH = "/catalog/v2.0/merchants/%s/products/status";
    private static final String BATCH_PATH = "/catalog/v2.0/merchants/%s/batch/%s";
    private static final String OPTIONS_PRICE_PATH = "/catalog/v2.0/merchants/%s/options/price";
    private static final String OPTIONS_STATUS_PATH = "/catalog/v2.0/merchants/%s/options/status";
    private static final String OPTION_GROUP_STATUS_PATH = "/catalog/v2.0/merchants/%s/optionGroups/%s/status";

    @ConfigProperty(name = "biteflow.ifood.base-url")
    String baseUrl;

    @Inject
    ObjectMapper objectMapper;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Override
    public List<String> listCatalogIds(String token, String merchantId) {
        String fullUrl = baseUrl + String.format(CATALOGS_PATH, merchantId);
        try {
            LOG.infof("[IFOOD-API] GET %s", fullUrl);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(fullUrl))
                    .header("Authorization", "Bearer " + token)
                    .GET()
                    .timeout(Duration.ofSeconds(15))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            LOG.infof("[IFOOD-API] GET %s -> HTTP %d | response: %s", fullUrl, response.statusCode(), response.body());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                List<String> catalogIds = new ArrayList<>();
                JsonNode root = objectMapper.readTree(response.body());
                if (root.isArray()) {
                    for (JsonNode node : root) {
                        if (node.has("catalogId")) {
                            catalogIds.add(node.get("catalogId").asText());
                        } else if (node.has("id")) {
                            catalogIds.add(node.get("id").asText());
                        }
                    }
                }
                return catalogIds;
            }
            throw new IfoodIntegrationException(
                    "Erro ao listar catálogos no iFood: HTTP " + response.statusCode() + " | response: " + response.body());
        } catch (IfoodIntegrationException e) {
            throw e;
        } catch (Exception e) {
            LOG.errorf(e, "[IFOOD-API] Exceção ao chamar GET %s: %s", fullUrl, e.getMessage());
            throw new IfoodIntegrationException("Erro ao listar catálogos no iFood: " + e.getMessage(), e);
        }
    }

    @Override
    public String createCategory(String token, String merchantId, String catalogId, IfoodCreateCategoryRequest request) {
        return post(token, String.format(CATEGORIES_PATH, merchantId, catalogId), request);
    }

    @Override
    public String createProduct(String token, String merchantId, IfoodCreateProductRequest request) {
        // iFood v2.0 usa PUT /items para criação/atualização (idempotente)
        put(token, String.format(ITEMS_PATH, merchantId), request);
        return request.item().id(); // Retorna o ID que nós mesmos geramos/enviamos
    }

    @Override
    public void updateProductPrice(String token, String merchantId, String itemId, IfoodUpdatePriceRequest request) {
        patch(token, String.format(ITEM_BY_ID_PATH, merchantId, itemId), request);
    }

    @Override
    public void updateProductStatus(String token, String merchantId, String itemId, IfoodUpdateStatusRequest request) {
        patch(token, String.format(ITEM_BY_ID_PATH, merchantId, itemId), request);
    }

    @Override
    public void patchItem(String token, String merchantId, String itemId, IfoodItemPatchRequest request) {
        patch(token, String.format(ITEM_BY_ID_PATH, merchantId, itemId), request);
    }

    @Override
    public IfoodBatchResponse updateProductsPriceBatch(String token, String merchantId, List<IfoodBatchPriceItem> items) {
        return patchWithResponse(token, String.format(PRODUCTS_PRICE_PATH, merchantId), items, IfoodBatchResponse.class);
    }

    @Override
    public IfoodBatchResponse updateProductsStatusBatch(String token, String merchantId, List<IfoodBatchStatusItem> items) {
        return patchWithResponse(token, String.format(PRODUCTS_STATUS_PATH, merchantId), items, IfoodBatchResponse.class);
    }

    @Override
    public IfoodBatchStatusResponse getBatchStatus(String token, String merchantId, String batchId) {
        String fullUrl = baseUrl + String.format(BATCH_PATH, merchantId, batchId);
        try {
            LOG.infof("[IFOOD-API] GET %s", fullUrl);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(fullUrl))
                    .header("Authorization", "Bearer " + token)
                    .GET()
                    .timeout(Duration.ofSeconds(15))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            LOG.infof("[IFOOD-API] GET %s -> HTTP %d | response: %s", fullUrl, response.statusCode(), response.body());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return objectMapper.readValue(response.body(), IfoodBatchStatusResponse.class);
            }
            throw new IfoodIntegrationException(
                    "Erro ao buscar status do lote no iFood: HTTP " + response.statusCode() + " | response: " + response.body());
        } catch (IfoodIntegrationException e) {
            throw e;
        } catch (Exception e) {
            LOG.errorf(e, "[IFOOD-API] Exceção ao chamar GET %s: %s", fullUrl, e.getMessage());
            throw new IfoodIntegrationException("Erro ao buscar status do lote no iFood: " + e.getMessage(), e);
        }
    }

    @Override
    public void updateOptionPrice(String token, String merchantId, IfoodUpdateOptionPriceRequest request) {
        patch(token, String.format(OPTIONS_PRICE_PATH, merchantId), request);
    }

    @Override
    public void updateOptionStatus(String token, String merchantId, IfoodUpdateOptionStatusRequest request) {
        patch(token, String.format(OPTIONS_STATUS_PATH, merchantId), request);
    }

    @Override
    public void updateOptionGroupStatus(String token, String merchantId, String optionGroupId, IfoodUpdateOptionGroupStatusRequest request) {
        patch(token, String.format(OPTION_GROUP_STATUS_PATH, merchantId, optionGroupId), request);
    }



    private String put(String token, String path, Object body) {
        String fullUrl = baseUrl + path;
        try {
            String bodyJson = objectMapper.writeValueAsString(body);
            LOG.infof("[IFOOD-API] PUT %s | body: %s", fullUrl, bodyJson);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(fullUrl))
                    .header("Authorization", "Bearer " + token)
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(bodyJson))
                    .timeout(Duration.ofSeconds(30))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            LOG.infof("[IFOOD-API] PUT %s -> HTTP %d | response: %s", fullUrl, response.statusCode(), response.body());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return extractIdFromResponse(response.body());
            }

            // 409 Conflict = recurso já existe no iFood
            if (response.statusCode() == 409) {
                String existingId = extractConflictingResourceId(response.body());
                if (existingId != null) {
                    LOG.infof("[IFOOD-API] Item já existe no iFood (409 Conflict). Usando ID existente: %s", existingId);
                    return existingId;
                }
            }

            throw new IfoodIntegrationException(
                    "Erro ao criar item no iFood: HTTP " + response.statusCode() + " | response: " + response.body());
        } catch (IfoodIntegrationException e) {
            throw e;
        } catch (Exception e) {
            LOG.errorf(e, "[IFOOD-API] Exceção ao chamar PUT %s: %s", fullUrl, e.getMessage());
            throw new IfoodIntegrationException("Erro ao chamar API do iFood (PUT " + path + "): " + e.getMessage(), e);
        }
    }

    private String post(String token, String path, Object body) {
        String fullUrl = baseUrl + path;
        try {
            String bodyJson = objectMapper.writeValueAsString(body);
            LOG.infof("[IFOOD-API] POST %s | body: %s", fullUrl, bodyJson);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(fullUrl))
                    .header("Authorization", "Bearer " + token)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(bodyJson))
                    .timeout(Duration.ofSeconds(15))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            LOG.infof("[IFOOD-API] POST %s -> HTTP %d | response: %s", fullUrl, response.statusCode(), response.body());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return extractIdFromResponse(response.body());
            }

            // 409 Conflict = recurso já existe no iFood. Extrair o ID do recurso existente.
            if (response.statusCode() == 409) {
                String existingId = extractConflictingResourceId(response.body());
                if (existingId != null) {
                    LOG.infof("[IFOOD-API] Recurso já existe no iFood (409 Conflict). Usando ID existente: %s", existingId);
                    return existingId;
                }
                LOG.warnf("[IFOOD-API] 409 Conflict mas não foi possível extrair o ID do recurso existente. Response: %s", response.body());
            }

            throw new IfoodIntegrationException(
                    "Erro ao criar no iFood: HTTP " + response.statusCode() + " | response: " + response.body());
        } catch (IfoodIntegrationException e) {
            throw e;
        } catch (Exception e) {
            LOG.errorf(e, "[IFOOD-API] Exceção ao chamar POST %s: %s", fullUrl, e.getMessage());
            throw new IfoodIntegrationException("Erro ao chamar API do iFood (POST " + path + "): " + e.getMessage(), e);
        }
    }

    private void patch(String token, String path, Object body) {
        String fullUrl = baseUrl + path;
        try {
            String bodyJson = objectMapper.writeValueAsString(body);
            LOG.infof("[IFOOD-API] PATCH %s | body: %s", fullUrl, bodyJson);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(fullUrl))
                    .header("Authorization", "Bearer " + token)
                    .header("Content-Type", "application/json")
                    .method("PATCH", HttpRequest.BodyPublishers.ofString(bodyJson))
                    .timeout(Duration.ofSeconds(15))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            LOG.infof("[IFOOD-API] PATCH %s -> HTTP %d | response: %s", fullUrl, response.statusCode(), response.body());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return;
            }
            throw new IfoodIntegrationException(
                    "Erro ao atualizar no iFood: HTTP " + response.statusCode() + " | response: " + response.body());
        } catch (IfoodIntegrationException e) {
            throw e;
        } catch (Exception e) {
            LOG.errorf(e, "[IFOOD-API] Exceção ao chamar PATCH %s: %s", fullUrl, e.getMessage());
            throw new IfoodIntegrationException("Erro ao chamar API do iFood (PATCH " + path + "): " + e.getMessage(), e);
        }
    }

    private <T> T patchWithResponse(String token, String path, Object body, Class<T> responseType) {
        String fullUrl = baseUrl + path;
        try {
            String bodyJson = objectMapper.writeValueAsString(body);
            LOG.infof("[IFOOD-API] PATCH %s | body: %s", fullUrl, bodyJson);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(fullUrl))
                    .header("Authorization", "Bearer " + token)
                    .header("Content-Type", "application/json")
                    .method("PATCH", HttpRequest.BodyPublishers.ofString(bodyJson))
                    .timeout(Duration.ofSeconds(30))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            LOG.infof("[IFOOD-API] PATCH %s -> HTTP %d | response: %s", fullUrl, response.statusCode(), response.body());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return objectMapper.readValue(response.body(), responseType);
            }
            throw new IfoodIntegrationException(
                    "Erro ao atualizar em lote no iFood: HTTP " + response.statusCode() + " | response: " + response.body());
        } catch (IfoodIntegrationException e) {
            throw e;
        } catch (Exception e) {
            LOG.errorf(e, "[IFOOD-API] Exceção ao chamar PATCH %s: %s", fullUrl, e.getMessage());
            throw new IfoodIntegrationException("Erro ao chamar API do iFood (PATCH " + path + "): " + e.getMessage(), e);
        }
    }

    private String extractIdFromResponse(String responseBody) {
        try {
            var node = objectMapper.readTree(responseBody);
            // PUT /items retorna {"item":{"id":"..."}, ...}
            if (node.has("item") && node.get("item").has("id")) {
                return node.get("item").get("id").asText();
            }
            // POST /categories retorna {"id":"..."}
            if (node.has("id")) {
                return node.get("id").asText();
            }
            // Fallback: se o body todo for um UUID, retorna ele
            String trimmed = responseBody.trim().replaceAll("\"", "");
            if (trimmed.matches("[0-9a-fA-F\\-]{36}")) {
                return trimmed;
            }
            LOG.warnf("[IFOOD-API] Não foi possível extrair ID da resposta: %s", responseBody);
            return responseBody;
        } catch (Exception e) {
            return responseBody;
        }
    }

    /**
     * Extrai o ID do recurso existente de uma resposta 409 Conflict do iFood.
     * Formato esperado: {"error":{"conflictingResources":["id-existente"]}}
     */
    private String extractConflictingResourceId(String responseBody) {
        try {
            var node = objectMapper.readTree(responseBody);
            var errorNode = node.has("error") ? node.get("error") : node;
            if (errorNode.has("conflictingResources")) {
                var resources = errorNode.get("conflictingResources");
                if (resources.isArray() && !resources.isEmpty()) {
                    return resources.get(0).asText();
                }
            }
            return null;
        } catch (Exception e) {
            LOG.warnf("Erro ao parsear conflictingResources: %s", e.getMessage());
            return null;
        }
    }
}
