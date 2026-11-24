package com.tradebridge.country.api.dto.request;

import com.tradebridge.validation.annotation.ValidCountryCode;
import com.tradebridge.validation.annotation.ValidCurrencyCode;
import com.tradebridge.validation.annotation.ValidLanguageCode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateCountryRequest(
        @NotBlank @Pattern(regexp = "^[A-Za-z]{2}$", message = "iso2 must be 2 letters") String iso2Code,
        @NotBlank @Pattern(regexp = "^[A-Za-z]{3}$", message = "iso3 must be 3 letters") String iso3Code,
        @NotBlank @Size(max = 150) String name,
        @Size(max = 100) String region,
        @Size(max = 10) String callingCode,
        @NotBlank @ValidCurrencyCode String defaultCurrencyCode,
        @NotBlank @ValidLanguageCode(allowBcp47 = false) String defaultLanguageCode,
        @NotNull Boolean enabled
) { }