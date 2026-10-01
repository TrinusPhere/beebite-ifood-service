package com.fidecode.biteflowifood.infrastructure.http;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fidecode.biteflowifood.application.dto.biteflow.BiteflowCatalogRequest.*;
import com.fidecode.biteflowifood.application.dto.biteflow.BiteflowCatalogResponse.*;
import com.fidecode.biteflowifood.application.port.BiteflowCatalogClient;
import com.fidecode.biteflowifood.domain.shared.exception.IfoodIntegrationException;
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
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class BiteflowApiClient implements BiteflowCatalogClient {

    private static final Logger LOG = Logger.getLogger(BiteflowApiClient.class);

    private static final String CATEGORIES_PATH = "/api/v1/categories";
    private static final String PRODUCTS_PATH = "/api/v1/products";
    private static final String MODIFIER_GROUPS_PATH = "/api/v1/modifier-groups";

    @ConfigProperty(name = "biteflow.api.base-url", defaultValue = "http://localhost:8080")
    String baseUrl;

    @Inject
    ObjectMapper objectMapper;

    @jakarta.ws.rs.core.Context
    jakarta.ws.rs.core.HttpHeaders httpHeaders;

    private String getAuthHeader() {
        if (httpHeaders != null) {
            String authHeader = httpHeaders.getHeaderString("Authorization");
            if (authHeader != null && !authHeader.isBlank()) {
                return authHeader;
            }
            
            // Extract from Cookie if header is not present
            var cookies = httpHeaders.getCookies();
            if (cookies != null && cookies.containsKey("biteflow_jwt")) {
                String cookieVal = cookies.get("biteflow_jwt").getValue();
                if (cookieVal != null && cookieVal.startsWith("Bearer")) {
                    return cookieVal;
                }
                return "Bearer " + cookieVal;
            }
        }
        return null;
    }

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Override
    public String createCategory(UUID storeId, CreateCategoryRequest request) {
        return post(storeId, CATEGORIES_PATH, request);
    }

    @Override
    public String createProduct(UUID storeId, CreateProductRequest request) {
        return post(storeId, PRODUCTS_PATH, request);
    }

    @Override
    public void updateProduct(UUID storeId, UUID productId, CreateProductRequest request) {
        try {
            String bodyJson = objectMapper.writeValueAsString(request);
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + PRODUCTS_PATH + "/" + productId))
                    .header("X-Store-Id", storeId.toString())
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(bodyJson))
                    .timeout(Duration.ofSeconds(15));

            String authHeader = getAuthHeader();
            if (authHeader != null) requestBuilder.header("Authorization", authHeader);

            HttpRequest req = requestBuilder.build();
            HttpResponse<String> response = httpClient.send(req, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 400) {
                throw new IfoodIntegrationException("Erro ao atualizar produto no BiteFlow: HTTP " + response.statusCode());
            }
        } catch (IfoodIntegrationException e) {
            throw e;
        } catch (Exception e) {
            throw new IfoodIntegrationException("Erro ao chamar API do BiteFlow (PUT Produto) -> " + e.getClass().getSimpleName() + ": " + e.getMessage(), e);
        }
    }

    @Override
    public String createModifierGroup(UUID storeId, CreateModifierGroupRequest request) {
        return post(storeId, MODIFIER_GROUPS_PATH, request);
    }

    @Override
    public List<CategoryResponse> listCategories(UUID storeId) {
        return getList(storeId, CATEGORIES_PATH, new TypeReference<>() {});
    }

    @Override
    public ProductResponse getProduct(UUID storeId, UUID productId) {
        try {
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + PRODUCTS_PATH + "/" + productId))
                    .header("X-Store-Id", storeId.toString())
                    .GET()
                    .timeout(Duration.ofSeconds(15));
            
            String authHeader = getAuthHeader();
            if (authHeader != null) requestBuilder.header("Authorization", authHeader);

            HttpRequest request = requestBuilder.build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return objectMapper.readValue(response.body(), ProductResponse.class);
            }
            if (response.statusCode() == 404) return null;
            throw new IfoodIntegrationException("Erro ao buscar produto no BiteFlow: HTTP " + response.statusCode());
        } catch (IfoodIntegrationException e) {
            throw e;
        } catch (Exception e) {
            throw new IfoodIntegrationException("Erro ao chamar API do BiteFlow (GET Produto) -> " + e.getClass().getSimpleName() + ": " + e.getMessage(), e);
        }
    }

    @Override
    public List<ProductResponse> listProducts(UUID storeId, String categoryId) {
        String path = PRODUCTS_PATH;
        if (categoryId != null && !categoryId.isBlank()) {
            path += "?categoryId=" + categoryId;
        }
        return getList(storeId, path, new TypeReference<>() {});
    }

    @Override
    public List<ModifierGroupResponse> listModifierGroups(UUID storeId) {
        return getList(storeId, MODIFIER_GROUPS_PATH, new TypeReference<>() {});
    }

    @Override
    public ProductResponse findProductByExternalCode(UUID storeId, String externalCode) {
        if (externalCode == null || externalCode.isBlank()) return null;
        LOG.infof("[BITEFLOW-CLIENT] Buscando produto por externalCode='%s' (endpoint dedicado)", externalCode);
        try {
            // Encode do externalCode para URL (pode conter caracteres especiais como '-')
            String encodedCode = java.net.URLEncoder.encode(externalCode, java.nio.charset.StandardCharsets.UTF_8)
                    .replace("+", "%20");
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + PRODUCTS_PATH + "/by-external-code/" + encodedCode))
                    .header("X-Store-Id", storeId.toString())
                    .GET()
                    .timeout(Duration.ofSeconds(15));

            String authHeader = getAuthHeader();
            if (authHeader != null) requestBuilder.header("Authorization", authHeader);

            HttpResponse<String> response = httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                ProductResponse found = objectMapper.readValue(response.body(), ProductResponse.class);
                LOG.infof("[BITEFLOW-CLIENT] Produto encontrado: id=%s, externalCode='%s'", found.id(), found.externalCode());
                return found;
            }
            if (response.statusCode() == 404) {
                LOG.warnf("[BITEFLOW-CLIENT] Produto com externalCode='%s' N\u00c3O encontrado (404).", externalCode);
                return null;
            }
            LOG.warnf("[BITEFLOW-CLIENT] Busca por externalCode retornou HTTP %d", response.statusCode());
            return null;
        } catch (Exception e) {
            LOG.warnf("[BITEFLOW-CLIENT] Erro ao buscar por externalCode: %s", e.getMessage());
            return null;
        }
    }

    /** POST genérico. Em caso de 409 Conflict (duplicate), busca o produto existente pelo externalCode e retorna seu ID. */
    private String post(UUID storeId, String path, Object body) {
        try {
            String bodyJson = objectMapper.writeValueAsString(body);

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + path))
                    .header("Content-Type", "application/json")
                    .header("X-Store-Id", storeId.toString())
                    .POST(HttpRequest.BodyPublishers.ofString(bodyJson))
                    .timeout(Duration.ofSeconds(15));

            String authHeader = getAuthHeader();
            if (authHeader != null) requestBuilder.header("Authorization", authHeader);

            HttpRequest request = requestBuilder.build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                // The actual Biteflow API returns the created DTO with an 'id' field.
                JsonNode root = objectMapper.readTree(response.body());
                if (root.has("id")) {
                    return root.get("id").asText();
                }
                return response.body();
            }
            // 409 Conflict ou 500 causado por duplicate key: tenta recuperar o produto existente pelo externalCode
            if ((response.statusCode() == 409 || response.statusCode() == 500) && PRODUCTS_PATH.equals(path)) {
                LOG.warnf("[BITEFLOW-CLIENT] POST produto retornou HTTP %d. Tentando recuperar produto existente pelo externalCode...", response.statusCode());
                try {
                    JsonNode reqBody = objectMapper.readTree(bodyJson);
                    String extCode = reqBody.has("externalCode") ? reqBody.get("externalCode").asText() : null;
                    if (extCode != null && !extCode.isBlank()) {
                        ProductResponse existing = findProductByExternalCode(storeId, extCode);
                        if (existing != null) {
                            LOG.infof("[BITEFLOW-CLIENT] Produto recuperado via externalCode='%s': id=%s", extCode, existing.id());
                            return existing.id();
                        }
                    }
                } catch (Exception ignored) {
                    LOG.warnf("[BITEFLOW-CLIENT] Falha ao tentar recuperar produto existente: %s", ignored.getMessage());
                }
            }
            throw new IfoodIntegrationException("Erro ao criar entidade no BiteFlow: HTTP " + response.statusCode());
        } catch (IfoodIntegrationException e) {
            throw e;
        } catch (Exception e) {
            throw new IfoodIntegrationException("Erro ao chamar API do BiteFlow (POST) -> " + e.getClass().getSimpleName() + ": " + e.getMessage(), e);
        }
    }

    private <T> List<T> getList(UUID storeId, String path, TypeReference<List<T>> typeRef) {
        try {
            List<T> allResults = new ArrayList<>();
            int page = 0;
            int pageSize = 50;
            boolean hasMore = true;

            while (hasMore) {
                String separator = path.contains("?") ? "&" : "?";
                String paginatedPath = path + separator + "page=" + page + "&size=" + pageSize;

                HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                        .uri(URI.create(baseUrl + paginatedPath))
                        .header("X-Store-Id", storeId.toString())
                        .GET()
                        .timeout(Duration.ofSeconds(15));

                String authHeader = getAuthHeader();
                if (authHeader != null) requestBuilder.header("Authorization", authHeader);

                HttpRequest request = requestBuilder.build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    JsonNode root = objectMapper.readTree(response.body());
                    JsonNode contentNode = root.has("content") ? root.get("content") : root;
                    List<T> pageContent = objectMapper.readValue(contentNode.toString(), typeRef);
                    allResults.addAll(pageContent);

                    // Check if there are more pages
                    int totalPages = root.has("totalPages") ? root.get("totalPages").asInt() : 1;
                    page++;
                    hasMore = page < totalPages;
                } else if (response.statusCode() == 204 || response.statusCode() == 404) {
                    return allResults;
                } else {
                    throw new IfoodIntegrationException("Erro ao buscar dados no BiteFlow: HTTP " + response.statusCode());
                }
            }

            return allResults;
        } catch (IfoodIntegrationException e) {
            throw e;
        } catch (Exception e) {
            throw new IfoodIntegrationException("Erro ao chamar API do BiteFlow (GET) -> " + e.getClass().getSimpleName() + ": " + e.getMessage(), e);
        }
    }
}
