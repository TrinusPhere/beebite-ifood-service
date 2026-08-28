package com.fidecode.biteflowifood.application.dto;

import java.util.List;

public record GetStatusResponse(
        boolean isOpen,
        List<IfoodMerchantStatusResponse> operations
) {}
