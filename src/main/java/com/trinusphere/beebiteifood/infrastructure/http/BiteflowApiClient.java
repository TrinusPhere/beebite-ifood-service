package com.trinusphere.beebiteifood.infrastructure.http;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trinusphere.beebiteifood.application.dto.biteflow.BiteflowCatalogRequest.*;
import com.trinusphere.beebiteifood.application.dto.biteflow.BiteflowCatalogResponse.*;
import com.trinusphere.beebiteifood.application.port.BiteflowCatalogClient;
import com.trinusphere.beebiteifood.domain.shared.exception.IfoodIntegrationException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class BiteflowApiClient implements BiteflowCatalogClient {

    private static final Logger LOG = Logger.getLogger(BiteflowApiClient.class);

    private static final String INTERNAL_CATALOG_BASE = "/api/v1/internal/catalog";
    private static final String STORES_CONTEXT_PATH = INTERNAL_CATALOG_BASE + "/stores/";
    private static final String CATEGORIES_PATH = INTERNAL_CATALOG_BASE + "/categories";
    private static final String PRODUCTS_PATH = INTERNAL_CATALOG_BASE + "/products";
    private static final String MODIFIER_GROUPS_PATH = INTERNAL_CATALOG_BASE + "/modifier-groups";

    @ConfigProperty(name = "biteflow.api.base-url", defaultValue = "http://localhost:8080")
    String baseUrl;

    @ConfigProperty(name = "biteflow.internal.service-token", defaultValue = "beebite-internal-dev-token")
    String internalServiceToken;

    @Inject
    ObjectMapper objectMapper;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private HttpRequest.Builder baseInternalRequest(String path, UUID storeId) {
        String normalizedBase = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(normalizedBase + path))
                .header("X-Internal-Service-Token", internalServiceToken != null ? internalServiceToken.trim() : "")
                .timeout(Duration.ofSeconds(15));
        if (storeId != null) {
            builder.header("X-Store-Id", storeId.toString());
        }
        return builder;
    }

    @Override
    public StoreCatalogContextResponse getStoreCatalogContext(UUID storeId) {
        try {
            HttpRequest request = baseInternalRequest(STORES_CONTEXT_PATH + storeId + "/context", storeId)
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return objectMapper.readValue(response.body(), StoreCatalogContextResponse.class);
            }
            if (response.statusCode() == 404) {
                return null;
            }
            throw new IfoodIntegrationException("Erro ao consultar contexto de catálogo da loja no BeeBite: HTTP " + response.statusCode());
        } catch (IfoodIntegrationException e) {
            throw e;
        } catch (Exception e) {
            throw new IfoodIntegrationException("Erro ao chamar API interna do BeeBite (StoreContext) -> " + e.getMessage(), e);
        }
    }

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
            HttpRequest req = baseInternalRequest(PRODUCTS_PATH + "/" + productId, storeId)
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(bodyJson))
                    .build();

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
            HttpRequest request = baseInternalRequest(PRODUCTS_PATH + "/" + productId, storeId)
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return objectMapper.readValue(response.body(), ProductResponse.class);
            }
            if (response.statusCode() == 404) {
                return null;
            }
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
            String encodedCode = URLEncoder.encode(externalCode, StandardCharsets.UTF_8).replace("+", "%20");
            HttpRequest request = baseInternalRequest(PRODUCTS_PATH + "/by-external-code/" + encodedCode, storeId)
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return objectMapper.readValue(response.body(), ProductResponse.class);
            }
            if (response.statusCode() == 404) {
                return null;
            }
            LOG.warnf("[BITEFLOW-CLIENT] Busca por externalCode retornou HTTP %d", response.statusCode());
            return null;
        } catch (Exception e) {
            LOG.warnf("[BITEFLOW-CLIENT] Erro ao buscar por externalCode: %s", e.getMessage());
            return null;
        }
    }

    private String post(UUID storeId, String path, Object body) {
        try {
            String bodyJson = objectMapper.writeValueAsString(body);
            HttpRequest request = baseInternalRequest(path, storeId)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(bodyJson))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                JsonNode root = objectMapper.readTree(response.body());
                if (root.has("id")) {
                    return root.get("id").asText();
                }
                return response.body();
            }
            if ((response.statusCode() == 409 || response.statusCode() == 500) && PRODUCTS_PATH.equals(path)) {
                try {
                    JsonNode reqBody = objectMapper.readTree(bodyJson);
                    String extCode = reqBody.has("externalCode") ? reqBody.get("externalCode").asText() : null;
                    if (extCode != null && !extCode.isBlank()) {
                        ProductResponse existing = findProductByExternalCode(storeId, extCode);
                        if (existing != null) {
                            return existing.id();
                        }
                    }
                } catch (Exception ignored) {
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

                HttpRequest request = baseInternalRequest(paginatedPath, storeId)
                        .GET()
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    JsonNode root = objectMapper.readTree(response.body());
                    JsonNode contentNode = root.has("content") ? root.get("content") : root;
                    List<T> pageContent = objectMapper.readValue(contentNode.toString(), typeRef);
                    allResults.addAll(pageContent);

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
