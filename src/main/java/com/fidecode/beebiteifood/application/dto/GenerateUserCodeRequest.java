package com.fidecode.beebiteifood.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record GenerateUserCodeRequest(
        @NotBlank String clientId
) {}
