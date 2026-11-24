package com.tradebridge.country.api.dto.response;

import java.time.Instant;
import java.util.List;

/**
 * Full country view including supported currencies and languages.
 */
public record CountryDetailResponse(
        String iso2Code,
        String iso3Code,
        String name,
        String region,
        String callingCode,
        String defaultCurrencyCode,
        String defaultLanguageCode,
        boolean enabled,
        List<CurrencyResponse> currencies,
        List<LanguageResponse> languages,
        Instant createdAt,
        Instant updatedAt
) { }