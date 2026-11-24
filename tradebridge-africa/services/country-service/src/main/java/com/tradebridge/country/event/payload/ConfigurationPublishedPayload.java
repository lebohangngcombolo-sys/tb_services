package com.tradebridge.country.event.payload;

/**
 * {@code country.configuration.published.v1} payload.
 */
public record ConfigurationPublishedPayload(
        String countryCode,
        int version
) { }