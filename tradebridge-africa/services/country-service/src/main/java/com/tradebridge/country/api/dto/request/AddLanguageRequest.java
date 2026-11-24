package com.tradebridge.country.api.dto.request;

import com.tradebridge.validation.annotation.ValidLanguageCode;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AddLanguageRequest(
        @NotNull @ValidLanguageCode(allowBcp47 = false) String languageCode,
        @Size(max = 100) String name,
        @Size(max = 100) String nativeName,
        @NotNull Boolean isDefault
) { }