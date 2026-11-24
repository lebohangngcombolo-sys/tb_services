package com.tradebridge.customer.client.dto;

import java.util.List;
import java.util.UUID;

/**
 * Response of {@code GET /api/v1/countries/{code}/kyc-requirements} from
 * country-service. Flattened for our use.
 */
public record KycRequirementsResponse(
        List<Item> requirements
) {
    public record Item(
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
}