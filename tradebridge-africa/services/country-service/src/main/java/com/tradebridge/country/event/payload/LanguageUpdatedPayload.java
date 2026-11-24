package com.tradebridge.country.event.payload;

/**
 * {@code country.language.updated.v1} payload.
 */
public record LanguageUpdatedPayload(
        String countryCode,
        String languageCode
) { }