package com.tradebridge.customer.client.dto;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Response of {@code GET /api/v1/countries/{code}/organization-requirements}.
 */
public record OrganizationRequirementsResponse(
        List<Item> requirements
) {
    public record Item(
            UUID id,
            String countryCode,
            String organizationType,
            String requirementCode,
            String description,
            boolean required,
            Map<String, Object> validationRule,
            boolean enabled
    ) { }
}