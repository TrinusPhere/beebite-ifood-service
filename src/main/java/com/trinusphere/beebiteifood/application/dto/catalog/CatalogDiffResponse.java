package com.trinusphere.beebiteifood.application.dto.catalog;

import com.trinusphere.beebiteifood.application.dto.biteflow.BiteflowCatalogResponse.ProductResponse;

import java.util.List;

public record CatalogDiffResponse(
        List<CatalogDiffEntry> entries,
        int totalMatches,
        int totalConflicts,
        int totalBiteflowOnly,
        int totalIfoodOnly
) {
    public record CatalogDiffEntry(
            String diffStatus,           // MATCH, CONFLICT, BITEFLOW_ONLY, IFOOD_ONLY
            ProductResponse biteflowItem, // null se IFOOD_ONLY
            IfoodItemPresentation ifoodItem, // null se BITEFLOW_ONLY
            List<String> conflictFields  // ["price", "name", "status"] se CONFLICT
    ) {}

    public record IfoodItemPresentation(
            String id, 
            String name, 
            IfoodCatalogResponse.IfoodPrice price, 
            String status
    ) {}
}
