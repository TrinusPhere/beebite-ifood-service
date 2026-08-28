package com.fidecode.biteflowifood.application.dto;

public record IfoodMerchantStatusResponse(
        String operation,
        boolean available,
        String state
) {}
