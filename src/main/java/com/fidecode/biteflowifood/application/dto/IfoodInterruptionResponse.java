package com.fidecode.biteflowifood.application.dto;

public record IfoodInterruptionResponse(
        String id,
        String description,
        String start,
        String end
) {}
