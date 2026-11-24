package com.tradebridge.country.mapper;

import com.tradebridge.country.api.dto.response.CountryDetailResponse;
import com.tradebridge.country.api.dto.response.CountryResponse;
import com.tradebridge.country.api.dto.response.CurrencyResponse;
import com.tradebridge.country.api.dto.response.LanguageResponse;
import com.tradebridge.country.domain.Country;

import java.util.List;

/**
 * Mapper for country summaries and details. Stateless.
 */
public final class CountryMapper {

    public CountryResponse toSummary(Country country) {
        if (country == null) return null;
        return new CountryResponse(
                country.getIso2Code(),
                country.getIso3Code(),
                country.getName(),
                country.getRegion(),
                country.getCallingCode(),
                country.getDefaultCurrencyCode(),
                country.getDefaultLanguageCode(),
                country.isEnabled());
    }

    public CountryDetailResponse toDetail(Country country,
                                          List<CurrencyResponse> currencies,
                                          List<LanguageResponse> languages) {
        if (country == null) return null;
        return new CountryDetailResponse(
                country.getIso2Code(),
                country.getIso3Code(),
                country.getName(),
                country.getRegion(),
                country.getCallingCode(),
                country.getDefaultCurrencyCode(),
                country.getDefaultLanguageCode(),
                country.isEnabled(),
                currencies == null ? List.of() : currencies,
                languages == null ? List.of() : languages,
                country.getCreatedAt(),
                country.getUpdatedAt());
    }
}