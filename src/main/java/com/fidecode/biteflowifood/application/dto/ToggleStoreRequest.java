package com.fidecode.biteflowifood.application.dto;

public record ToggleStoreRequest(
        String action, // OPEN, CLOSE
        String description
) {}
