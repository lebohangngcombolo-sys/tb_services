package com.tradebridge.country.api.dto.response;

public record CurrencyResponse(
        String code,
        String name,
        String symbol,
        int decimalPlaces,
        boolean enabled,
        boolean isDefault
) { }