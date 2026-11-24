package com.tradebridge.customer.api.dto.response;

import java.util.List;

/**
 * Flattened view of a country KYC requirement — specifically what the
 * *current KYC case* requires based on the country configuration version
 * that was locked in at case creation (spec §11 decision 3).
 */
public record KycRequirementResponse(
        String documentType,
        String requirementCode,
        String description,
        boolean required,
        List<String> allowedMimeTypes,
        Long maxFileSizeBytes,
        Integer validityDays,
        boolean satisfied
) { }