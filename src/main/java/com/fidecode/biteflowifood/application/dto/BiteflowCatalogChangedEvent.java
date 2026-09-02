package com.fidecode.biteflowifood.application.dto;

import java.util.UUID;

public record BiteflowCatalogChangedEvent(
        UUID storeId,
        UUID productId,
        String changeType
) {}
