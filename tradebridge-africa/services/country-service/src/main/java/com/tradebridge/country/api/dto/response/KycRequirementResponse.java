package com.tradebridge.country.api.dto.response;

import java.util.List;
import java.util.UUID;

public record KycRequirementResponse(
        UUID id,
        String countryCode,
        String customerType,
        String documentType,
        String requirementCode,
        String description,
        boolean required,
        List<String> allowedMimeTypes,
        Long maxFileSizeBytes,
        Integer validityDays,
        boolean enabled
) { }