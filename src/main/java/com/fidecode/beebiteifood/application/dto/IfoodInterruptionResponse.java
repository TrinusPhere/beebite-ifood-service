package com.fidecode.beebiteifood.application.dto;

public record IfoodInterruptionResponse(
        String id,
        String description,
        String start,
        String end
) {}
