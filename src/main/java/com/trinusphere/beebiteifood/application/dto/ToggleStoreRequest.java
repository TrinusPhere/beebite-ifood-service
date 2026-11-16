package com.trinusphere.beebiteifood.application.dto;

public record ToggleStoreRequest(
        String action, // OPEN, CLOSE
        String description
) {}
