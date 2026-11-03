package com.fidecode.biteflowifood.application.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record IfoodAlternativeDisputeRequest(
        String type,
        Map<String, Object> metadata
) {}
