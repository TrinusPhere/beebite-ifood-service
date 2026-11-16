package com.trinusphere.beebiteifood.application.dto;

import java.util.UUID;

public record BiteflowCatalogChangedEvent(
        UUID storeId,
        UUID productId,
        String changeType
) {}
