package com.tradebridge.country.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public record KycRequirementRequest(
        @NotBlank @Size(max = 30) String customerType,
        @NotBlank @Size(max = 60) String documentType,
        @NotBlank @Size(max = 100) String requirementCode,
        String description,
        @NotNull Boolean required,
        List<String> allowedMimeTypes,
        @Positive Long maxFileSizeBytes,
        Integer validityDays,
        @NotNull Boolean enabled
) { }