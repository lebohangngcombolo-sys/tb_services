package com.tradebridge.country.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record RollbackConfigurationRequest(
        @NotNull @Positive Integer targetVersion,
        @NotBlank String reason
) { }