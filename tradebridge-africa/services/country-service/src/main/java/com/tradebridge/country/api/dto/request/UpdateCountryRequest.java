package com.tradebridge.country.api.dto.request;

import com.tradebridge.validation.annotation.ValidCurrencyCode;
import com.tradebridge.validation.annotation.ValidLanguageCode;
import jakarta.validation.constraints.Size;

/**
 * All fields optional — null means "leave unchanged".
 */
public record UpdateCountryRequest(
        @Size(max = 150) String name,
        @Size(max = 100) String region,
        @Size(max = 10) String callingCode,
        @ValidCurrencyCode String defaultCurrencyCode,
        @ValidLanguageCode(allowBcp47 = false) String defaultLanguageCode
) { }