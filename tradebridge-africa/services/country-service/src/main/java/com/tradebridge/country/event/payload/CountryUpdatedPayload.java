package com.tradebridge.country.event.payload;

/**
 * {@code country.updated.v1} payload. {@code changedFields} is a
 * comma-separated list of field names.
 */
public record CountryUpdatedPayload(
        String countryCode,
        String changedFields
) { }