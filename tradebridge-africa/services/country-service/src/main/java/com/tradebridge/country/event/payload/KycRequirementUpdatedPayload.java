package com.tradebridge.country.event.payload;

/**
 * {@code country.kyc.requirement.updated.v1} payload.
 */
public record KycRequirementUpdatedPayload(
        String countryCode,
        String requirementCode
) { }