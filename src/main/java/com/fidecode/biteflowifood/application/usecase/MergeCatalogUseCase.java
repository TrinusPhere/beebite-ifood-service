package com.fidecode.biteflowifood.application.usecase;

import com.fidecode.biteflowifood.domain.catalog.entity.CatalogMapping;
import com.fidecode.biteflowifood.domain.catalog.repository.CatalogMappingRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class MergeCatalogUseCase {

    @Inject
    CatalogMappingRepository mappingRepository;
    
    // @Inject
    // ImportCatalogFromIfoodUseCase importUseCase; // For single item imports
    
    // @Inject
    // ExportCatalogToIfoodUseCase exportUseCase; // For single item exports

    public void execute(UUID storeId, String merchantId, String token, MergeRequest request) {
        for (MergeDecision decision : request.decisions()) {
            switch (decision.action()) {
                case "USE_BITEFLOW":
                    // Atualiza no iFood com os dados do BiteFlow
                    // Se for CONFLICT, a opção USE_BITEFLOW significa que o iFood vai receber o PATCH do preço/status do BiteFlow
                    // TODO: call ifoodCatalogSyncClient.updateItemPrice / updateItemStatus based on difference
                    break;
                case "USE_IFOOD":
                    // Atualiza no BiteFlow com os dados do iFood
                    // Se for CONFLICT, o BiteFlow recebe PATCH com preço do iFood
                    // TODO: call biteflowCatalogClient to update product price
                    break;
                case "IMPORT":
                    // Era IFOOD_ONLY, cria no BiteFlow
                    // TODO: Chamar lógica de importação apenas para este ifoodItemId
                    break;
                case "EXPORT":
                    // Era BITEFLOW_ONLY, cria no iFood
                    // TODO: Chamar lógica de exportação apenas para este biteflowProductId
                    break;
                case "IGNORE":
                    // Não faz nada, apenas marca como lido ou deixa como está
                    break;
                case "LINK_ONLY":
                    // Era MATCH ou o usuário decidiu ignorar a diferença e apenas vincular os IDs
                    if (decision.ifoodItemId() != null && decision.biteflowProductId() != null) {
                        mappingRepository.save(new CatalogMapping(storeId, decision.ifoodItemId(), UUID.fromString(decision.biteflowProductId()), "PRODUCT"));
                    }
                    break;
            }
        }
    }

    public record MergeRequest(
            List<MergeDecision> decisions
    ) {}

    public record MergeDecision(
            String ifoodItemId,
            String biteflowProductId,
            String action // USE_BITEFLOW, USE_IFOOD, IMPORT, EXPORT, IGNORE, LINK_ONLY
    ) {}
}
