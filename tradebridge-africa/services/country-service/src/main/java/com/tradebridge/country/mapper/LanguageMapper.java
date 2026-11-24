package com.tradebridge.country.mapper;

import com.tradebridge.country.api.dto.response.LanguageResponse;
import com.tradebridge.country.domain.Language;

public final class LanguageMapper {

    public LanguageResponse toResponse(Language language, boolean isDefault) {
        if (language == null) return null;
        return new LanguageResponse(
                language.getCode(),
                language.getName(),
                language.getNativeName(),
                language.isEnabled(),
                isDefault);
    }
}