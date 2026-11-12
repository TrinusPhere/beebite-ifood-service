package com.fidecode.beebiteifood.application.dto;

public record IfoodMerchantStatusResponse(
        String operation,
        boolean available,
        String state
) {}
