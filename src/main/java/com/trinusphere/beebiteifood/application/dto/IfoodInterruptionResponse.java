package com.trinusphere.beebiteifood.application.dto;

public record IfoodInterruptionResponse(
        String id,
        String description,
        String start,
        String end
) {}
