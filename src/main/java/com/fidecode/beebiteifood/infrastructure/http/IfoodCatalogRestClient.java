package com.fidecode.beebiteifood.infrastructure.http;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fidecode.beebiteifood.application.dto.catalog.IfoodCatalogResponse.*;
import com.fidecode.beebiteifood.application.port.IfoodCatalogClient;
import com.fidecode.beebiteifood.domain.shared.exception.IfoodIntegrationException;
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

@ApplicationScoped
public class IfoodCatalogRestClient implements IfoodCatalogClient {

    private static final String CATALOGS_PATH = "/catalog/v2.0/merchants/%s/catalogs";
    private static final String CATEGORIES_PATH = "/catalog/v2.0/merchants/%s/catalogs/%s/categories";
    private static final String CATEGORY_ITEMS_PATH = "/catalog/v2.0/merchants/%s/categories/%s/items";
    private static final String ITEM_PATH = "/catalog/v2.0/merchants/%s/items/%s";
    private static final String SELLABLE_ITEMS_PATH = "/catalog/v2.0/merchants/%s/catalogs/%s/sellableItems";
    private static final String UNSELLABLE_ITEMS_PATH = "/catalog/v2.0/merchants/%s/catalogs/%s/unsellableItems";

    @ConfigProperty(name = "biteflow.ifood.base-url")
    String baseUrl;

    @Inject
    ObjectMapper objectMapper;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Override
    public List<IfoodCatalog> listCatalogs(String token, String merchantId) {
        return getList(token, String.format(CATALOGS_PATH, merchantId), new TypeReference<>() {});
    }

    @Override
    public List<IfoodCategory> listCategories(String token, String merchantId) {
        List<IfoodCatalog> catalogs = listCatalogs(token, merchantId);
        if (catalogs == null || catalogs.isEmpty()) return Collections.emptyList();
        String catalogId = catalogs.get(0).catalogId();
        return getList(token, String.format(CATEGORIES_PATH, merchantId, catalogId), new TypeReference<>() {});
    }

    @Override
    public IfoodCategoryItemsResponse listCategoryItems(String token, String merchantId, String categoryId) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + String.format(CATEGORY_ITEMS_PATH, merchantId, categoryId)))
                .header("Authorization", "Bearer " + token)
                .GET()
                .timeout(Duration.ofSeconds(15))
                .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return objectMapper.readValue(response.body(), IfoodCategoryItemsResponse.class);
            }
            if (response.statusCode() == 404) return null;
            throw new IfoodIntegrationException("Erro ao buscar items da categoria: HTTP " + response.statusCode() + " | " + response.body());
        } catch (IfoodIntegrationException e) {
            throw e;
        } catch (Exception e) {
            throw new IfoodIntegrationException("Erro ao chamar API de Catálogo -> " + e.getClass().getSimpleName() + ": " + e.getMessage(), e);
        }
    }

    @Override
    public IfoodItemFull getItem(String token, String merchantId, String itemId) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + String.format(ITEM_PATH, merchantId, itemId)))
                .header("Authorization", "Bearer " + token)
                .GET()
                .timeout(Duration.ofSeconds(15))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return objectMapper.readValue(response.body(), IfoodItemFull.class);
            }
            if (response.statusCode() == 404) return null;
            throw new IfoodIntegrationException("Erro ao buscar item no catálogo: HTTP " + response.statusCode());
        } catch (Exception e) {
            throw new IfoodIntegrationException("Erro ao chamar API de Catálogo", e);
        }
    }

    @Override
    public List<IfoodItemData> listSellableItems(String token, String merchantId) {
        List<IfoodCatalog> catalogs = listCatalogs(token, merchantId);
        if (catalogs == null || catalogs.isEmpty()) return Collections.emptyList();
        String catalogId = catalogs.get(0).catalogId();
        return getList(token, String.format(SELLABLE_ITEMS_PATH, merchantId, catalogId), new TypeReference<>() {});
    }

    @Override
    public List<IfoodUnsellableCategory> listUnsellableItems(String token, String merchantId) {
        List<IfoodCatalog> catalogs = listCatalogs(token, merchantId);
        if (catalogs == null || catalogs.isEmpty()) return Collections.emptyList();
        String catalogId = catalogs.get(0).catalogId();
        return getList(token, String.format(UNSELLABLE_ITEMS_PATH, merchantId, catalogId), new TypeReference<>() {});
    }

    private <T> List<T> getList(String token, String path, TypeReference<List<T>> typeReference) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .header("Authorization", "Bearer " + token)
                .GET()
                .timeout(Duration.ofSeconds(15))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return objectMapper.readValue(response.body(), typeReference);
            }
            if (response.statusCode() == 204 || response.statusCode() == 404) return Collections.emptyList();
            throw new IfoodIntegrationException("Erro ao buscar catálogo: HTTP " + response.statusCode() + " | " + response.body());
        } catch (IfoodIntegrationException e) {
            throw e;
        } catch (Exception e) {
            throw new IfoodIntegrationException("Erro ao chamar API de Catálogo -> " + e.getClass().getSimpleName() + ": " + e.getMessage(), e);
        }
    }
}
