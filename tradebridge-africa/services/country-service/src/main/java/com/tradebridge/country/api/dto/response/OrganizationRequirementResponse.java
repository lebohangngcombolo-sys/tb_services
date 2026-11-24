package com.tradebridge.country.api.dto.response;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record OrganizationRequirementResponse(
        UUID id,
        String countryCode,
        String organizationType,
        String requirementCode,
        String description,
        boolean required,
        Map<String, Object> validationRule,
        Instant effectiveFrom,
        Instant effectiveUntil,
        boolean enabled
) { }