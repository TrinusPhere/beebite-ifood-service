package com.trinusphere.beebiteifood.application.dto;

public record IfoodMerchantStatusResponse(
        String operation,
        boolean available,
        String state
) {}
