package com.tradebridge.country.event.payload;

/**
 * {@code country.organization.requirement.updated.v1} payload.
 */
public record OrganizationRequirementUpdatedPayload(
        String countryCode,
        String requirementCode
) { }