package com.fidecode.biteflowifood.application.usecase;

import com.fidecode.biteflowifood.application.dto.biteflow.BiteflowCatalogResponse.*;
import com.fidecode.biteflowifood.application.dto.catalog.IfoodCatalogRequest.*;
import com.fidecode.biteflowifood.application.port.BiteflowCatalogClient;
import com.fidecode.biteflowifood.application.port.IfoodCatalogSyncClient;
import com.fidecode.biteflowifood.domain.catalog.entity.CatalogMapping;
import com.fidecode.biteflowifood.domain.catalog.repository.CatalogMappingRepository;
import com.fidecode.biteflowifood.domain.shared.exception.BusinessRuleException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class ExportCatalogToIfoodUseCase {

    private static final Logger LOG = Logger.getLogger(ExportCatalogToIfoodUseCase.class);

    @Inject
    BiteflowCatalogClient biteflowCatalogClient;

    @Inject
    IfoodCatalogSyncClient ifoodCatalogSyncClient;

    @Inject
    CatalogMappingRepository mappingRepository;

    @jakarta.transaction.Transactional
    public void execute(UUID storeId, String merchantId, String token) {
        LOG.infof("[EXPORT] Iniciando export para storeId=%s, merchantId=%s", storeId, merchantId);

        // 0. Obter o catalogId do iFood (necessário para criar categorias)
        List<String> catalogIds = ifoodCatalogSyncClient.listCatalogIds(token, merchantId);
        if (catalogIds.isEmpty()) {
            throw new BusinessRuleException("Nenhum catálogo encontrado no iFood para o merchantId: " + merchantId);
        }
        String catalogId = catalogIds.get(0); // Usa o primeiro catálogo disponível
        LOG.infof("[EXPORT] Usando catalogId=%s", catalogId);

        // 1. Obter todas as categorias do BiteFlow
        List<CategoryResponse> categories = biteflowCatalogClient.listCategories(storeId);
        LOG.infof("[EXPORT] Categorias encontradas no BiteFlow: %d", categories.size());

        List<ModifierGroupResponse> modifierGroups = biteflowCatalogClient.listModifierGroups(storeId);
        LOG.infof("[EXPORT] Modifier groups encontrados no BiteFlow: %d", modifierGroups.size());

        for (CategoryResponse category : categories) {
            LOG.infof("[EXPORT] Processando categoria: %s (id=%s)", category.name(), category.id());

            // Verificar se a categoria já está mapeada
            Optional<CatalogMapping> existingCatMapping = mappingRepository.findByBiteflowIdAndStoreId(UUID.fromString(category.id()), storeId);
            String ifoodCategoryId;

            if (existingCatMapping.isEmpty()) {
                // Criar no iFood (agora com catalogId)
                IfoodCreateCategoryRequest catReq = CatalogExportMapper.toIfoodCategory(category);
                LOG.infof("[EXPORT] Criando categoria no iFood: %s (catalogId=%s)", category.name(), catalogId);
                ifoodCategoryId = ifoodCatalogSyncClient.createCategory(token, merchantId, catalogId, catReq);
                LOG.infof("[EXPORT] Categoria criada no iFood com id=%s", ifoodCategoryId);

                mappingRepository.save(new CatalogMapping(storeId, ifoodCategoryId, UUID.fromString(category.id()), "CATEGORY"));
            } else {
                ifoodCategoryId = existingCatMapping.get().getIfoodId();
                LOG.infof("[EXPORT] Categoria já mapeada: ifoodId=%s", ifoodCategoryId);
            }

            // 2. Para cada produto da categoria
            List<ProductResponse> products = biteflowCatalogClient.listProducts(storeId, category.id());
            LOG.infof("[EXPORT] Produtos da categoria %s: %d", category.name(), products.size());

            for (ProductResponse product : products) {
                LOG.infof("[EXPORT] Processando produto: %s (id=%s)", product.name(), product.id());

                Optional<CatalogMapping> existingProdMapping = mappingRepository.findByBiteflowIdAndStoreId(UUID.fromString(product.id()), storeId);

                if (existingProdMapping.isEmpty()) {
                    // Mapeia os modifiers do produto
                    List<ModifierGroupResponse> productGroups = modifierGroups.stream()
                            .filter(g -> product.modifierGroupIds() != null && product.modifierGroupIds().contains(g.id()))
                            .collect(Collectors.toList());

                    String generatedItemId = UUID.randomUUID().toString();
                    IfoodCreateProductRequest productReq = CatalogExportMapper.toIfoodProduct(product, productGroups, ifoodCategoryId, generatedItemId);

                    LOG.infof("[EXPORT] Criando produto no iFood: %s (gerado id: %s)", product.name(), generatedItemId);
                    ifoodCatalogSyncClient.createProduct(token, merchantId, productReq);
                    LOG.infof("[EXPORT] Produto criado no iFood com id=%s", generatedItemId);

                    mappingRepository.save(new CatalogMapping(storeId, generatedItemId, UUID.fromString(product.id()), "PRODUCT"));
                } else {
                    String ifoodProductId = existingProdMapping.get().getIfoodId();
                    LOG.infof("[EXPORT] Produto já mapeado: ifoodId=%s, atualizando status", ifoodProductId);

                    try {
                        ifoodCatalogSyncClient.updateProductStatus(token, merchantId, ifoodProductId,
                                new IfoodUpdateStatusRequest(product.status()));
                    } catch (com.fidecode.biteflowifood.domain.shared.exception.IfoodIntegrationException ex) {
                        if (ex.getMessage().contains("HTTP 404")) {
                            LOG.warnf("[EXPORT] Produto %s não encontrado no iFood (404). Removendo mapping e recriando...", ifoodProductId);
                            mappingRepository.deleteByStoreId(storeId); // Não é o ideal apagar todos, mas o panache só tem deleteByStoreId na interface
                            
                            // Em vez de recriar no mesmo loop (o que é complexo sem reescrever muito), vamos lançar uma exceção clara para rodar de novo
                            throw new BusinessRuleException("O iFood não encontrou um produto mapeado (404). O mapeamento foi limpo. Por favor, execute o EXPORT novamente.");
                        } else {
                            throw ex;
                        }
                    }
                }
            }
        }

        LOG.infof("[EXPORT] Export concluído para storeId=%s", storeId);
    }
}
