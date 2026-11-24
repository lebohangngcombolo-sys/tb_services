package com.tradebridge.country.api.dto.response;

public record LanguageResponse(
        String code,
        String name,
        String nativeName,
        boolean enabled,
        boolean isDefault
) { }