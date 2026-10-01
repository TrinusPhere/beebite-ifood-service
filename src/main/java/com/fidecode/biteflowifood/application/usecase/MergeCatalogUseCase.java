package com.fidecode.biteflowifood.application.usecase;

import com.fidecode.biteflowifood.application.dto.biteflow.BiteflowCatalogRequest.CreateProductRequest;
import com.fidecode.biteflowifood.application.dto.biteflow.BiteflowCatalogRequest.ChannelPriceDto;
import com.fidecode.biteflowifood.application.dto.biteflow.BiteflowCatalogRequest.TechnicalSheetItemDto;
import com.fidecode.biteflowifood.application.dto.biteflow.BiteflowCatalogResponse.ProductResponse;
import com.fidecode.biteflowifood.application.dto.biteflow.BiteflowCatalogResponse.ModifierGroupResponse;
import com.fidecode.biteflowifood.application.dto.catalog.IfoodCatalogRequest.IfoodCreateProductRequest;
import com.fidecode.biteflowifood.application.dto.catalog.IfoodCatalogRequest.IfoodCreateCategoryRequest;
import com.fidecode.biteflowifood.application.dto.catalog.IfoodCatalogResponse.IfoodItemFull;
import com.fidecode.biteflowifood.application.dto.catalog.IfoodCatalogResponse.IfoodCategory;
import com.fidecode.biteflowifood.application.dto.biteflow.BiteflowCatalogResponse.CategoryResponse;
import com.fidecode.biteflowifood.application.port.BiteflowCatalogClient;
import com.fidecode.biteflowifood.application.port.IfoodCatalogClient;
import com.fidecode.biteflowifood.application.port.IfoodCatalogSyncClient;
import com.fidecode.biteflowifood.application.port.ImageStoragePort;
import com.fidecode.biteflowifood.domain.catalog.entity.CatalogMapping;
import com.fidecode.biteflowifood.domain.catalog.repository.CatalogMappingRepository;
import com.fidecode.biteflowifood.domain.shared.exception.BusinessRuleException;
import com.fidecode.biteflowifood.domain.shared.exception.IfoodIntegrationException;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
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
        boolean needIfoodItems = request.decisions().stream().anyMatch(d -> 
            "IMPORT".equals(d.action()) || "USE_BITEFLOW".equals(d.action()) || "USE_IFOOD".equals(d.action()));
            
        Map<String, IfoodItemFull> ifoodItemMap = new HashMap<>();
        Map<String, IfoodCategory> categoryMap = new HashMap<>();

        if (needIfoodItems) {
            populateIfoodMaps(token, merchantId, ifoodItemMap, categoryMap);
        }

        for (MergeDecision decision : request.decisions()) {
            try {
                processDecision(storeId, merchantId, token, decision, ifoodItemMap);
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
                    var itemProducts = new ArrayList<com.fidecode.biteflowifood.application.dto.catalog.IfoodCatalogResponse.IfoodProduct>();
                    if (categoryItems.products() != null) {
                        categoryItems.products().stream()
                            .filter(p -> p.id().equals(itemData.id()) || p.id().equals(itemData.productId()))
                            .forEach(itemProducts::add);
                    }
                    var itemOptGroups = new ArrayList<com.fidecode.biteflowifood.application.dto.catalog.IfoodCatalogResponse.IfoodOptionGroup>();
                    var itemOpts = new ArrayList<com.fidecode.biteflowifood.application.dto.catalog.IfoodCatalogResponse.IfoodOption>();

                    IfoodItemFull itemFull = new IfoodItemFull(itemData, itemProducts, itemOptGroups, itemOpts);
                    ifoodItemMap.put(itemData.id(), itemFull);
                }
            }
        }
    }

    private void processDecision(UUID storeId, String merchantId, String token, MergeDecision decision, Map<String, IfoodItemFull> ifoodItemMap) {
        switch (decision.action()) {
            case "USE_BITEFLOW":
                handleUseBiteflow(storeId, merchantId, token, decision, ifoodItemMap);
                break;
            case "USE_IFOOD":
                handleUseIfood(storeId, decision, ifoodItemMap);
                break;
            case "IMPORT":
                handleImport(storeId, merchantId, token, decision, ifoodItemMap);
                break;
            case "EXPORT":
                handleExport(storeId, merchantId, token, decision, ifoodItemMap);
                break;
            case "LINK_ONLY":
                handleLinkOnly(storeId, decision);
                break;
            case "IGNORE":
            default:
                break;
        }
    }

    private void handleUseBiteflow(UUID storeId, String merchantId, String token, MergeDecision decision, Map<String, IfoodItemFull> ifoodItemMap) {
        if (decision.ifoodItemId() == null || decision.biteflowProductId() == null) return;

        ProductResponse product = biteflowCatalogClient.getProduct(storeId, UUID.fromString(decision.biteflowProductId()));
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
            LOG.warn("[MERGE-USE_IFOOD] ifoodItemId ou biteflowProductId é null, abortando.");
            return;
        }

        LOG.infof("[MERGE-USE_IFOOD] Processando ifoodItemId=%s, biteflowProductId=%s", decision.ifoodItemId(), decision.biteflowProductId());
        LOG.infof("[MERGE-USE_IFOOD] ifoodItemMap contém %d itens. Keys: %s", ifoodItemMap.size(), ifoodItemMap.keySet());

        IfoodItemFull itemF = ifoodItemMap.get(decision.ifoodItemId());
        ProductResponse product = biteflowCatalogClient.getProduct(storeId, UUID.fromString(decision.biteflowProductId()));
        
        LOG.infof("[MERGE-USE_IFOOD] itemF=%s, product=%s", itemF != null ? "ENCONTRADO" : "NULL", product != null ? product.name() : "NULL");

        if (itemF != null && itemF.item() != null && product != null) {
            LOG.infof("[MERGE-USE_IFOOD] itemF.item().price()=%s, itemF.item().contextModifiers()=%s", 
                itemF.item().price(), itemF.item().contextModifiers());

            String name = itemF.products() != null && !itemF.products().isEmpty() ? itemF.products().get(0).name() : "Produto sem nome";
            String desc = itemF.products() != null && !itemF.products().isEmpty() ? itemF.products().get(0).description() : null;
            BigDecimal ifoodPrice = itemF.item().price() != null ? itemF.item().price().value() : BigDecimal.ZERO;
            String status = itemF.item().status() != null ? itemF.item().status() : "AVAILABLE";
            
            // Se o preço vier de contextModifiers (não diretamente no campo price), extrair de lá
            if (itemF.item().price() == null && itemF.item().contextModifiers() != null) {
                ifoodPrice = extractPriceFromContextModifiers(itemF.item().contextModifiers());
                LOG.infof("[MERGE-USE_IFOOD] Preço extraído de contextModifiers: %s", ifoodPrice);
            }

            LOG.infof("[MERGE-USE_IFOOD] name=%s, ifoodPrice=%s, status=%s", name, ifoodPrice, status);
            
            // Copiar channelPrices existentes, mas substituir/adicionar o canal IFOOD com o preço do iFood
            List<ChannelPriceDto> reqChannels = new ArrayList<>();
            boolean ifoodChannelFound = false;
            if (product.channelPrices() != null) {
                for (var cp : product.channelPrices()) {
                    if ("IFOOD".equals(cp.channel())) {
                        // Substituir preço do canal IFOOD pelo preço vindo do iFood
                        reqChannels.add(new ChannelPriceDto("IFOOD", ifoodPrice));
                        ifoodChannelFound = true;
                    } else {
                        reqChannels.add(new ChannelPriceDto(cp.channel(), cp.price()));
                    }
                }
            }
            if (!ifoodChannelFound) {
                // Se não existia canal IFOOD, adicionar com o preço do iFood
                reqChannels.add(new ChannelPriceDto("IFOOD", ifoodPrice));
            }

            LOG.infof("[MERGE-USE_IFOOD] channelPrices montados: %s", reqChannels);
            
            List<TechnicalSheetItemDto> reqTech = new ArrayList<>();
            if (product.technicalSheet() != null) {
                for (var ts : product.technicalSheet()) {
                    reqTech.add(new TechnicalSheetItemDto(ts.ingredientId(), ts.quantity()));
                }
            }
            
            CreateProductRequest req = new CreateProductRequest(
                    name, product.categoryId(), desc, ifoodPrice, product.imageUrl(), product.stock(),
                    status, product.externalCode(), product.catalogItemType(), reqChannels,
                    product.productType(), product.trackStock(), product.productionSectorId(),
                    product.saleUnit(), product.availableDigitalMenu(), product.availablePdv(),
                    reqTech, product.removableItemIds(), product.modifierGroupIds()
            );
            
            LOG.infof("[MERGE-USE_IFOOD] Chamando updateProduct para storeId=%s, productId=%s", storeId, product.id());
            biteflowCatalogClient.updateProduct(storeId, UUID.fromString(product.id()), req);
            LOG.info("[MERGE-USE_IFOOD] updateProduct concluído com sucesso.");
        } else {
            LOG.warnf("[MERGE-USE_IFOOD] Condição não atendida - itemF=%s, itemF.item()=%s, product=%s",
                itemF != null, itemF != null ? itemF.item() != null : "N/A", product != null);
        }
    }

    private BigDecimal extractPriceFromContextModifiers(com.fasterxml.jackson.databind.JsonNode contextModifiers) {
        if (contextModifiers != null && contextModifiers.isArray()) {
            for (com.fasterxml.jackson.databind.JsonNode modifier : contextModifiers) {
                String ctx = modifier.path("catalogContext").asText();
                if ("DEFAULT".equals(ctx) || "IFOOD".equals(ctx) || ctx.isBlank()) {
                    com.fasterxml.jackson.databind.JsonNode priceNode = modifier.path("price");
                    if (!priceNode.isMissingNode() && !priceNode.isNull()) {
                        BigDecimal value = priceNode.path("value").isMissingNode() ? null : new BigDecimal(priceNode.path("value").asText());
                        if (value != null) return value;
                    }
                }
            }
        }
        return BigDecimal.ZERO;
    }

    private void handleImport(UUID storeId, String merchantId, String token, MergeDecision decision, Map<String, IfoodItemFull> ifoodItemMap) {
        if (decision.ifoodItemId() == null) return;

        IfoodItemFull itemFull = ifoodItemMap.get(decision.ifoodItemId());
        if (itemFull == null || itemFull.item() == null) {
            throw new BusinessRuleException("Item " + decision.ifoodItemId() + " não encontrado no catálogo do iFood.");
        }

        String ifoodCategoryId = itemFull.item().categoryId();
        UUID biteflowCategoryId = getOrCreateBiteflowCategory(storeId, ifoodCategoryId, merchantId, token);

        String name = itemFull.products() != null && !itemFull.products().isEmpty() ? itemFull.products().get(0).name() : "Produto sem nome";
        String desc = itemFull.products() != null && !itemFull.products().isEmpty() ? itemFull.products().get(0).description() : null;
        BigDecimal price = itemFull.item().price() != null ? itemFull.item().price().value() : BigDecimal.ZERO;
        String status = itemFull.item().status() != null ? itemFull.item().status() : "AVAILABLE";
        String externalCode = itemFull.item().externalCode();

        Optional<CatalogMapping> existingProdMapping = mappingRepository.findByIfoodIdAndStoreId(decision.ifoodItemId(), storeId);
        
        if (existingProdMapping.isPresent()) {
            LOG.infof("[MERGE] Produto iFood %s já está mapeado para Biteflow %s. Atualizando status/preço no iFood.", 
                decision.ifoodItemId(), existingProdMapping.get().getBiteflowId());
            
            ProductResponse product = biteflowCatalogClient.getProduct(storeId, existingProdMapping.get().getBiteflowId());
            List<ModifierGroupResponse> productGroups = biteflowCatalogClient.listModifierGroups(storeId);
            
            try {
                String generatedItemId = decision.ifoodItemId();
                IfoodCreateProductRequest req = CatalogExportMapper.toIfoodProduct(product, productGroups, ifoodCategoryId, generatedItemId);
                ifoodCatalogSyncClient.createProduct(token, merchantId, req);
            } catch (IfoodIntegrationException ex) {
                if (ex.getMessage().contains("HTTP 404")) {
                    LOG.warnf("[MERGE] Produto %s não encontrado (404). Recriando do zero...", decision.ifoodItemId());
                    mappingRepository.delete(existingProdMapping.get());
                    createNewProductAndMap(storeId, merchantId, token, product, productGroups, ifoodCategoryId, decision.ifoodItemId());
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
                LOG.infof("[MERGE] Produto já existe no PDV com código %s. Criando mapeamento e atualizando iFood...", externalCode);
                mappingRepository.save(new CatalogMapping(storeId, decision.ifoodItemId(), UUID.fromString(existingByCode.id()), "PRODUCT"));
                
                List<ModifierGroupResponse> productGroups = biteflowCatalogClient.listModifierGroups(storeId);
                IfoodCreateProductRequest req = CatalogExportMapper.toIfoodProduct(existingByCode, productGroups, ifoodCategoryId, decision.ifoodItemId());
                ifoodCatalogSyncClient.createProduct(token, merchantId, req);
            } else {
                CreateProductRequest createReq = new CreateProductRequest(
                        name, biteflowCategoryId.toString(), desc, price, null, 0,
                        status, externalCode, "DEFAULT", List.of(), "RESALE", false, null, "UN", true, true, List.of(), null, List.of()
                );
                String newBiteflowProductId = biteflowCatalogClient.createProduct(storeId, createReq);
                mappingRepository.save(new CatalogMapping(storeId, decision.ifoodItemId(), UUID.fromString(newBiteflowProductId), "PRODUCT"));
            }
        }
    }

    private void handleLinkOnly(UUID storeId, MergeDecision decision) {
        if (decision.ifoodItemId() != null && decision.biteflowProductId() != null) {
            mappingRepository.save(new CatalogMapping(storeId, decision.ifoodItemId(), UUID.fromString(decision.biteflowProductId()), "PRODUCT"));
        }
    }

    private UUID getOrCreateBiteflowCategory(UUID storeId, String ifoodCategoryId, String merchantId, String token) {
        Optional<CatalogMapping> existingCatMapping = mappingRepository.findByIfoodIdAndStoreId(ifoodCategoryId, storeId);
        if (existingCatMapping.isPresent()) {
            return existingCatMapping.get().getBiteflowId();
        }
        
        // Categoria não mapeada, criar no Biteflow (simplificado para MVP)
        com.fidecode.biteflowifood.application.dto.biteflow.BiteflowCatalogRequest.CreateCategoryRequest catReq = 
            new com.fidecode.biteflowifood.application.dto.biteflow.BiteflowCatalogRequest.CreateCategoryRequest("Categoria Importada", "AVAILABLE", 0, List.of());
        String newBiteflowCatId = biteflowCatalogClient.createCategory(storeId, catReq);
        mappingRepository.save(new CatalogMapping(storeId, ifoodCategoryId, UUID.fromString(newBiteflowCatId), "CATEGORY"));
        return UUID.fromString(newBiteflowCatId);
    }

    private void createNewProductAndMap(UUID storeId, String merchantId, String token, ProductResponse product, List<ModifierGroupResponse> productGroups, String catIfoodId, String decisionIfoodItemId) {
        String generatedItemId = UUID.randomUUID().toString();
        IfoodCreateProductRequest req = CatalogExportMapper.toIfoodProduct(product, productGroups, catIfoodId, generatedItemId);
        String createdIfoodId = ifoodCatalogSyncClient.createProduct(token, merchantId, req);
        mappingRepository.save(new CatalogMapping(storeId, createdIfoodId, UUID.fromString(decisionIfoodItemId), "PRODUCT"));
    }

    private void handleExport(UUID storeId, String merchantId, String token, MergeDecision decision, Map<String, IfoodItemFull> ifoodItemMap) {
        if (decision.biteflowProductId() == null) return;
        
        ProductResponse product = biteflowCatalogClient.getProduct(storeId, UUID.fromString(decision.biteflowProductId()));
        if (product == null) {
            throw new BusinessRuleException("Produto Biteflow não encontrado.");
        }
        
        String ifoodCategoryId = getOrCreateIfoodCategory(storeId, product.categoryId(), merchantId, token);
        List<ModifierGroupResponse> productGroups = biteflowCatalogClient.listModifierGroups(storeId);
        
        String generatedItemId = UUID.randomUUID().toString();
        IfoodCreateProductRequest req = CatalogExportMapper.toIfoodProduct(product, productGroups, ifoodCategoryId, generatedItemId);
        
        String createdIfoodId = ifoodCatalogSyncClient.createProduct(token, merchantId, req);
        mappingRepository.save(new CatalogMapping(storeId, createdIfoodId, UUID.fromString(decision.biteflowProductId()), "PRODUCT"));
    }

    private String getOrCreateIfoodCategory(UUID storeId, String biteflowCategoryId, String merchantId, String token) {
        Optional<CatalogMapping> existingMapping = mappingRepository.findByBiteflowIdAndStoreId(UUID.fromString(biteflowCategoryId), storeId);
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
        String catalogId = catalogs.get(0);
        
        IfoodCreateCategoryRequest req = new IfoodCreateCategoryRequest(bfCat.name(), "AVAILABLE", "DEFAULT");
        String newIfoodCatId = ifoodCatalogSyncClient.createCategory(token, merchantId, catalogId, req);
        
        mappingRepository.save(new CatalogMapping(storeId, newIfoodCatId, UUID.fromString(biteflowCategoryId), "CATEGORY"));
        return newIfoodCatId;
    }

    public record MergeDecision(
            String ifoodItemId,
            String biteflowProductId,
            String action // "IMPORT", "EXPORT", "USE_BITEFLOW", "USE_IFOOD", "LINK_ONLY", "IGNORE"
    ) {}

    public record MergeRequest(
            List<MergeDecision> decisions
    ) {}
}
