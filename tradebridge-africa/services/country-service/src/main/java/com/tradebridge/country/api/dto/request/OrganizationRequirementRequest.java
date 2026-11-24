package com.tradebridge.country.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.Map;

public record OrganizationRequirementRequest(
        @NotBlank @Size(max = 50) String organizationType,
        @NotBlank @Size(max = 100) String requirementCode,
        String description,
        @NotNull Boolean required,
        Map<String, Object> validationRule,
        Instant effectiveFrom,
        Instant effectiveUntil,
        @NotNull Boolean enabled
) { }