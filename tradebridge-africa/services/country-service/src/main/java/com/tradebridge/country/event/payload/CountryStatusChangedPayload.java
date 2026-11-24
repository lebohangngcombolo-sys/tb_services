package com.tradebridge.country.event.payload;

/**
 * {@code country.status.changed.v1} payload.
 */
public record CountryStatusChangedPayload(
        String countryCode,
        boolean enabled
) { }