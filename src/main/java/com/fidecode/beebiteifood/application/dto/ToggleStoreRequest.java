package com.fidecode.beebiteifood.application.dto;

public record ToggleStoreRequest(
        String action, // OPEN, CLOSE
        String description
) {}
