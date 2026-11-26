package com.trinusphere.beebiteifood.application.usecase;

import com.fasterxml.jackson.databind.JsonNode;
import com.trinusphere.beebiteifood.application.dto.biteflow.BiteflowCatalogRequest.ChannelPriceDto;
import com.trinusphere.beebiteifood.application.dto.biteflow.BiteflowCatalogRequest.CreateCategoryRequest;
import com.trinusphere.beebiteifood.application.dto.biteflow.BiteflowCatalogRequest.CreateProductRequest;
import com.trinusphere.beebiteifood.application.dto.biteflow.BiteflowCatalogRequest.TechnicalSheetItemDto;
import com.trinusphere.beebiteifood.application.dto.biteflow.BiteflowCatalogResponse.CategoryResponse;
import com.trinusphere.beebiteifood.application.dto.biteflow.BiteflowCatalogResponse.ModifierGroupResponse;
import com.trinusphere.beebiteifood.application.dto.biteflow.BiteflowCatalogResponse.ProductResponse;
import com.trinusphere.beebiteifood.application.dto.catalog.IfoodCatalogRequest.IfoodCreateCategoryRequest;
import com.trinusphere.beebiteifood.application.dto.catalog.IfoodCatalogRequest.IfoodCreateProductRequest;
import com.trinusphere.beebiteifood.application.dto.catalog.IfoodCatalogResponse.IfoodCategory;
import com.trinusphere.beebiteifood.application.dto.catalog.IfoodCatalogResponse.IfoodItemFull;
import com.trinusphere.beebiteifood.application.port.BiteflowCatalogClient;
import com.trinusphere.beebiteifood.application.port.IfoodCatalogClient;
import com.trinusphere.beebiteifood.application.port.IfoodCatalogSyncClient;
import com.trinusphere.beebiteifood.application.port.ImageStoragePort;
import com.trinusphere.beebiteifood.domain.catalog.entity.CatalogMapping;
import com.trinusphere.beebiteifood.domain.catalog.repository.CatalogMappingRepository;
import com.trinusphere.beebiteifood.domain.shared.exception.BusinessRuleException;
import com.trinusphere.beebiteifood.domain.shared.exception.IfoodIntegrationException;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class MergeCatalogUseCase {

    private static final Logger LOG = Logger.getLogger(MergeCatalogUseCase.class);

    @Inject
    CatalogMappingRepository mappingRepository;

    @Inject
    BiteflowCatalogClient biteflowCatalogClient;

    @Inject
    IfoodCatalogSyncClient ifoodCatalogSyncClient;

    @Inject
    IfoodCatalogClient ifoodCatalogClient;

    @Inject
    ImageStoragePort imageStoragePort;

    public void execute(UUID storeId, String merchantId, String token, MergeRequest request) {
        execute(storeId, storeId, merchantId, token, request);
    }

    public void execute(UUID storeId, UUID catalogId, String merchantId, String token, MergeRequest request) {
        UUID effectiveCatalogId = catalogId != null ? catalogId : storeId;
        boolean needIfoodItems = request.decisions().stream().anyMatch(d ->
                "IMPORT".equals(d.action()) || "USE_BITEFLOW".equals(d.action()) || "USE_IFOOD".equals(d.action()));

        Map<String, IfoodItemFull> ifoodItemMap = new HashMap<>();
        Map<String, IfoodCategory> categoryMap = new HashMap<>();

        if (needIfoodItems) {
            populateIfoodMaps(token, merchantId, ifoodItemMap, categoryMap);
        }

        for (MergeDecision decision : request.decisions()) {
            try {
                processDecision(storeId, effectiveCatalogId, merchantId, token, decision, ifoodItemMap, categoryMap);
            } catch (Exception e) {
                LOG.errorf(e, "[MERGE] Falha ao processar decisão %s para item %s: %s",
                        decision.action(), decision.ifoodItemId(), e.getMessage());
                throw new IfoodIntegrationException("Erro ao processar merge do item: " + e.getMessage(), e);
            }
        }
    }

    private void populateIfoodMaps(String token, String merchantId, Map<String, IfoodItemFull> ifoodItemMap, Map<String, IfoodCategory> categoryMap) {
        var ifoodCategories = ifoodCatalogClient.listCategories(token, merchantId);
        for (var cat : ifoodCategories) {
            categoryMap.put(cat.id(), cat);
            var categoryItems = ifoodCatalogClient.listCategoryItems(token, merchantId, cat.id());
            if (categoryItems != null && categoryItems.items() != null) {
                for (var itemData : categoryItems.items()) {
                    var itemProducts = new ArrayList<com.trinusphere.beebiteifood.application.dto.catalog.IfoodCatalogResponse.IfoodProduct>();
                    if (categoryItems.products() != null) {
                        categoryItems.products().stream()
                                .filter(p -> p.id().equals(itemData.id()) || p.id().equals(itemData.productId()))
                                .forEach(itemProducts::add);
                    }
                    IfoodItemFull itemFull = new IfoodItemFull(itemData, itemProducts, new ArrayList<>(), new ArrayList<>());
                    ifoodItemMap.put(itemData.id(), itemFull);
                }
            }
        }
    }

    private void processDecision(
            UUID storeId,
            UUID catalogId,
            String merchantId,
            String token,
            MergeDecision decision,
            Map<String, IfoodItemFull> ifoodItemMap,
            Map<String, IfoodCategory> categoryMap) {
        switch (decision.action()) {
            case "USE_BITEFLOW":
                handleUseBiteflow(storeId, merchantId, token, decision, ifoodItemMap);
                break;
            case "USE_IFOOD":
                handleUseIfood(storeId, decision, ifoodItemMap);
                break;
            case "IMPORT":
                handleImport(storeId, catalogId, merchantId, token, decision, ifoodItemMap, categoryMap);
                break;
            case "EXPORT":
                handleExport(storeId, catalogId, merchantId, token, decision);
                break;
            case "LINK_ONLY":
                handleLinkOnly(storeId, catalogId, decision);
                break;
            case "IGNORE":
            default:
                break;
        }
    }

    private void handleUseBiteflow(UUID storeId, String merchantId, String token, MergeDecision decision, Map<String, IfoodItemFull> ifoodItemMap) {
        if (decision.ifoodItemId() == null || decision.biteflowProductId() == null) return;

        ProductResponse product = biteflowCatalogClient.getProduct(storeId, UUID.fromString(decision.biteflowProductId()));
        if (product == null) {
            throw new BusinessRuleException("Produto Biteflow " + decision.biteflowProductId() + " não encontrado (pode ter sido excluído no PDV). Recarregue a comparação do catálogo.");
        }
        IfoodItemFull itemFull = ifoodItemMap.get(decision.ifoodItemId());

        if (itemFull == null || itemFull.item() == null || itemFull.item().categoryId() == null) {
            throw new IfoodIntegrationException("Não foi possível determinar a categoria do item no iFood para a ação USE_BITEFLOW do produto " + product.name());
        }

        String categoryId = itemFull.item().categoryId();
        IfoodCreateProductRequest req = CatalogExportMapper.toIfoodProduct(product, null, categoryId, decision.ifoodItemId());
        ifoodCatalogSyncClient.createProduct(token, merchantId, req);
    }

    private void handleUseIfood(UUID storeId, MergeDecision decision, Map<String, IfoodItemFull> ifoodItemMap) {
        if (decision.ifoodItemId() == null || decision.biteflowProductId() == null) {
            return;
        }

        IfoodItemFull itemF = ifoodItemMap.get(decision.ifoodItemId());
        ProductResponse product = biteflowCatalogClient.getProduct(storeId, UUID.fromString(decision.biteflowProductId()));

        if (itemF != null && itemF.item() != null && product != null) {
            String name = itemF.products() != null && !itemF.products().isEmpty() ? itemF.products().get(0).name() : "Produto sem nome";
            String desc = itemF.products() != null && !itemF.products().isEmpty() ? itemF.products().get(0).description() : product.description();
            BigDecimal ifoodPrice = itemF.item().price() != null ? itemF.item().price().value() : BigDecimal.ZERO;
            String status = itemF.item().status() != null ? itemF.item().status() : "AVAILABLE";

            if (itemF.item().price() == null && itemF.item().contextModifiers() != null) {
                ifoodPrice = extractPriceFromContextModifiers(itemF.item().contextModifiers());
            }

            List<ChannelPriceDto> reqChannels = new ArrayList<>();
            boolean ifoodChannelFound = false;
            if (product.channelPrices() != null) {
                for (var cp : product.channelPrices()) {
                    if ("IFOOD".equals(cp.channel())) {
                        reqChannels.add(new ChannelPriceDto("IFOOD", ifoodPrice));
                        ifoodChannelFound = true;
                    } else {
                        reqChannels.add(new ChannelPriceDto(cp.channel(), cp.price()));
                    }
                }
            }
            if (!ifoodChannelFound) {
                reqChannels.add(new ChannelPriceDto("IFOOD", ifoodPrice));
            }

            List<TechnicalSheetItemDto> reqTech = new ArrayList<>();
            if (product.technicalSheet() != null) {
                for (var ts : product.technicalSheet()) {
                    reqTech.add(new TechnicalSheetItemDto(ts.ingredientId(), ts.quantity()));
                }
            }

            // Preserva o preço base do Balcão/PDV (product.price) no cardápio compartilhado e aplica o preço iFood em channelPrices['IFOOD']
            BigDecimal preservedBasePrice = product.price() != null ? product.price() : ifoodPrice;

            CreateProductRequest req = new CreateProductRequest(
                    name,
                    product.categoryId(),
                    desc,
                    preservedBasePrice,
                    product.estimatedCost(),
                    product.imageUrl(),
                    product.stock(),
                    status,
                    product.externalCode(),
                    product.catalogItemType(),
                    reqChannels,
                    product.productType(),
                    product.trackStock(),
                    product.productionSectorId(),
                    product.saleUnit(),
                    product.availableDigitalMenu(),
                    product.availablePdv(),
                    reqTech,
                    product.removableItemIds(),
                    product.modifierGroupIds()
            );

            biteflowCatalogClient.updateProduct(storeId, UUID.fromString(product.id()), req);
        }
    }

    private BigDecimal extractPriceFromContextModifiers(JsonNode contextModifiers) {
        if (contextModifiers != null && contextModifiers.isArray()) {
            for (JsonNode modifier : contextModifiers) {
                String ctx = modifier.path("catalogContext").asText();
                if ("DEFAULT".equals(ctx) || "IFOOD".equals(ctx) || ctx.isBlank()) {
                    JsonNode priceNode = modifier.path("price");
                    if (!priceNode.isMissingNode() && !priceNode.isNull()) {
                        BigDecimal value = priceNode.path("value").isMissingNode() ? null : new BigDecimal(priceNode.path("value").asText());
                        if (value != null) return value;
                    }
                }
            }
        }
        return BigDecimal.ZERO;
    }

    private void handleImport(
            UUID storeId,
            UUID catalogId,
            String merchantId,
            String token,
            MergeDecision decision,
            Map<String, IfoodItemFull> ifoodItemMap,
            Map<String, IfoodCategory> categoryMap) {
        if (decision.ifoodItemId() == null) return;

        IfoodItemFull itemFull = ifoodItemMap.get(decision.ifoodItemId());
        if (itemFull == null || itemFull.item() == null) {
            throw new BusinessRuleException("Item " + decision.ifoodItemId() + " não encontrado no catálogo do iFood.");
        }

        String ifoodCategoryId = itemFull.item().categoryId();
        IfoodCategory ifoodCategory = categoryMap.get(ifoodCategoryId);
        String ifoodCategoryName = ifoodCategory != null && ifoodCategory.name() != null ? ifoodCategory.name() : "Categoria Importada";
        UUID biteflowCategoryId = getOrCreateBiteflowCategory(storeId, catalogId, ifoodCategoryId, ifoodCategoryName);

        String name = itemFull.products() != null && !itemFull.products().isEmpty() ? itemFull.products().get(0).name() : "Produto sem nome";
        String desc = itemFull.products() != null && !itemFull.products().isEmpty() ? itemFull.products().get(0).description() : null;
        BigDecimal price = itemFull.item().price() != null ? itemFull.item().price().value() : BigDecimal.ZERO;
        String status = itemFull.item().status() != null ? itemFull.item().status() : "AVAILABLE";
        String externalCode = itemFull.item().externalCode();

        Optional<CatalogMapping> existingProdMapping = findMappingByIfoodId(decision.ifoodItemId(), catalogId, storeId);

        ProductResponse mappedProduct = null;
        if (existingProdMapping.isPresent()) {
            mappedProduct = biteflowCatalogClient.getProduct(storeId, existingProdMapping.get().getBiteflowId());
            if (mappedProduct == null) {
                mappingRepository.delete(existingProdMapping.get());
            }
        }

        if (mappedProduct != null) {
            List<ModifierGroupResponse> productGroups = biteflowCatalogClient.listModifierGroups(storeId);
            try {
                IfoodCreateProductRequest req = CatalogExportMapper.toIfoodProduct(mappedProduct, productGroups, ifoodCategoryId, decision.ifoodItemId());
                ifoodCatalogSyncClient.createProduct(token, merchantId, req);
            } catch (IfoodIntegrationException ex) {
                if (ex.getMessage().contains("HTTP 404")) {
                    mappingRepository.delete(existingProdMapping.get());
                    createNewProductAndMap(storeId, catalogId, merchantId, token, mappedProduct, productGroups, ifoodCategoryId);
                } else {
                    throw ex;
                }
            }
        } else {
            ProductResponse existingByCode = null;
            if (externalCode != null && !externalCode.isBlank()) {
                existingByCode = biteflowCatalogClient.findProductByExternalCode(storeId, externalCode);
            }

            if (existingByCode != null) {
                mappingRepository.save(new CatalogMapping(catalogId, storeId, decision.ifoodItemId(), UUID.fromString(existingByCode.id()), "PRODUCT"));
                List<ModifierGroupResponse> productGroups = biteflowCatalogClient.listModifierGroups(storeId);
                IfoodCreateProductRequest req = CatalogExportMapper.toIfoodProduct(existingByCode, productGroups, ifoodCategoryId, decision.ifoodItemId());
                ifoodCatalogSyncClient.createProduct(token, merchantId, req);
            } else {
                List<ChannelPriceDto> channelPrices = List.of(new ChannelPriceDto("IFOOD", price));
                CreateProductRequest createReq = new CreateProductRequest(
                        name, biteflowCategoryId.toString(), desc, price, null, null, 0,
                        status, externalCode, "DEFAULT", channelPrices, "RESALE", false, null, "UN", true, true, List.of(), null, List.of()
                );
                String newBiteflowProductId = biteflowCatalogClient.createProduct(storeId, createReq);
                mappingRepository.save(new CatalogMapping(catalogId, storeId, decision.ifoodItemId(), UUID.fromString(newBiteflowProductId), "PRODUCT"));
            }
        }
    }

    private void handleLinkOnly(UUID storeId, UUID catalogId, MergeDecision decision) {
        if (decision.ifoodItemId() == null || decision.biteflowProductId() == null) return;

        UUID biteflowProductId = UUID.fromString(decision.biteflowProductId());
        ProductResponse product = biteflowCatalogClient.getProduct(storeId, biteflowProductId);
        if (product == null) {
            throw new BusinessRuleException("Produto BiteFlow " + decision.biteflowProductId()
                    + " não encontrado (pode ter sido excluído no PDV). Recarregue a comparação do catálogo.");
        }

        Optional<CatalogMapping> existing = findMappingByIfoodId(decision.ifoodItemId(), catalogId, storeId);
        if (existing.isPresent()) {
            if (biteflowProductId.equals(existing.get().getBiteflowId())) {
                return;
            }
            mappingRepository.delete(existing.get());
        }
        mappingRepository.save(new CatalogMapping(catalogId, storeId, decision.ifoodItemId(), biteflowProductId, "PRODUCT"));
    }

    private UUID getOrCreateBiteflowCategory(UUID storeId, UUID catalogId, String ifoodCategoryId, String categoryName) {
        Optional<CatalogMapping> existingCatMapping = findMappingByIfoodId(ifoodCategoryId, catalogId, storeId);
        if (existingCatMapping.isPresent()) {
            String mappedId = existingCatMapping.get().getBiteflowId().toString();
            boolean stillExists = biteflowCatalogClient.listCategories(storeId).stream()
                    .anyMatch(c -> mappedId.equals(c.id()));
            if (stillExists) {
                return existingCatMapping.get().getBiteflowId();
            }
            mappingRepository.delete(existingCatMapping.get());
        }

        CreateCategoryRequest catReq = new CreateCategoryRequest(categoryName, "AVAILABLE", 0, List.of());
        String newBiteflowCatId = biteflowCatalogClient.createCategory(storeId, catReq);
        mappingRepository.save(new CatalogMapping(catalogId, storeId, ifoodCategoryId, UUID.fromString(newBiteflowCatId), "CATEGORY"));
        return UUID.fromString(newBiteflowCatId);
    }

    private void createNewProductAndMap(
            UUID storeId,
            UUID catalogId,
            String merchantId,
            String token,
            ProductResponse product,
            List<ModifierGroupResponse> productGroups,
            String catIfoodId) {
        String generatedItemId = UUID.randomUUID().toString();
        IfoodCreateProductRequest req = CatalogExportMapper.toIfoodProduct(product, productGroups, catIfoodId, generatedItemId);
        String createdIfoodId = ifoodCatalogSyncClient.createProduct(token, merchantId, req);
        mappingRepository.save(new CatalogMapping(catalogId, storeId, createdIfoodId, UUID.fromString(product.id()), "PRODUCT"));
    }

    private void handleExport(UUID storeId, UUID catalogId, String merchantId, String token, MergeDecision decision) {
        if (decision.biteflowProductId() == null) return;

        ProductResponse product = biteflowCatalogClient.getProduct(storeId, UUID.fromString(decision.biteflowProductId()));
        if (product == null) {
            throw new BusinessRuleException("Produto Biteflow não encontrado.");
        }

        String ifoodCategoryId = getOrCreateIfoodCategory(storeId, catalogId, product.categoryId(), merchantId, token);
        List<ModifierGroupResponse> productGroups = biteflowCatalogClient.listModifierGroups(storeId);

        String generatedItemId = UUID.randomUUID().toString();
        IfoodCreateProductRequest req = CatalogExportMapper.toIfoodProduct(product, productGroups, ifoodCategoryId, generatedItemId);

        String createdIfoodId = ifoodCatalogSyncClient.createProduct(token, merchantId, req);
        mappingRepository.save(new CatalogMapping(catalogId, storeId, createdIfoodId, UUID.fromString(decision.biteflowProductId()), "PRODUCT"));
    }

    private String getOrCreateIfoodCategory(UUID storeId, UUID catalogId, String biteflowCategoryId, String merchantId, String token) {
        Optional<CatalogMapping> existingMapping = findMappingByBiteflowId(UUID.fromString(biteflowCategoryId), catalogId, storeId);
        if (existingMapping.isPresent()) {
            return existingMapping.get().getIfoodId();
        }

        List<CategoryResponse> categories = biteflowCatalogClient.listCategories(storeId);
        CategoryResponse bfCat = categories.stream()
                .filter(c -> c.id().equals(biteflowCategoryId))
                .findFirst()
                .orElseThrow(() -> new BusinessRuleException("Categoria " + biteflowCategoryId + " não encontrada no Biteflow."));

        List<String> catalogs = ifoodCatalogSyncClient.listCatalogIds(token, merchantId);
        if (catalogs == null || catalogs.isEmpty()) {
            throw new IfoodIntegrationException("Nenhum catálogo encontrado no iFood para a loja " + merchantId);
        }
        String ifoodCatalogId = catalogs.get(0);

        IfoodCreateCategoryRequest req = new IfoodCreateCategoryRequest(bfCat.name(), "AVAILABLE", "DEFAULT");
        String newIfoodCatId = ifoodCatalogSyncClient.createCategory(token, merchantId, ifoodCatalogId, req);

        mappingRepository.save(new CatalogMapping(catalogId, storeId, newIfoodCatId, UUID.fromString(biteflowCategoryId), "CATEGORY"));
        return newIfoodCatId;
    }

    private Optional<CatalogMapping> findMappingByIfoodId(String ifoodId, UUID catalogId, UUID storeId) {
        Optional<CatalogMapping> byCatalog = mappingRepository.findByIfoodIdAndCatalogId(ifoodId, catalogId);
        return byCatalog.isPresent() ? byCatalog : mappingRepository.findByIfoodIdAndStoreId(ifoodId, storeId);
    }

    private Optional<CatalogMapping> findMappingByBiteflowId(UUID biteflowId, UUID catalogId, UUID storeId) {
        Optional<CatalogMapping> byCatalog = mappingRepository.findByBiteflowIdAndCatalogId(biteflowId, catalogId);
        return byCatalog.isPresent() ? byCatalog : mappingRepository.findByBiteflowIdAndStoreId(biteflowId, storeId);
    }

    public record MergeDecision(
            String ifoodItemId,
            String biteflowProductId,
            String action
    ) {}

    public record MergeRequest(
            List<MergeDecision> decisions
    ) {}
}
