package com.tradebridge.country.api.dto.response;

/**
 * Lightweight country summary for lists.
 */
public record CountryResponse(
        String iso2Code,
        String iso3Code,
        String name,
        String region,
        String callingCode,
        String defaultCurrencyCode,
        String defaultLanguageCode,
        boolean enabled
) { }