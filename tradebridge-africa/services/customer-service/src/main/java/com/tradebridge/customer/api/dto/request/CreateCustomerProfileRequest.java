package com.tradebridge.customer.api.dto.request;

import com.tradebridge.validation.annotation.ValidCountryCode;
import com.tradebridge.validation.annotation.ValidLanguageCode;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record CreateCustomerProfileRequest(
        @NotNull @Pattern(regexp = "^(INDIVIDUAL|COMPANY_REPRESENTATIVE)$") String customerType,
        @NotNull @ValidCountryCode(allowAlpha3 = false) String countryCode,
        @NotNull @ValidLanguageCode(allowBcp47 = false) String preferredLanguage
) { }