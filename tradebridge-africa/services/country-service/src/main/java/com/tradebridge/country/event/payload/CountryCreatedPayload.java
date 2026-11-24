package com.tradebridge.country.event.payload;

/**
 * {@code country.created.v1} payload.
 */
public record CountryCreatedPayload(
        String countryCode,
        String name
) { }