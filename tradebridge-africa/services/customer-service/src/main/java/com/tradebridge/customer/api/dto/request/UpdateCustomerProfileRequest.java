package com.tradebridge.customer.api.dto.request;

import com.tradebridge.validation.annotation.ValidLanguageCode;
import jakarta.validation.constraints.Size;

public record UpdateCustomerProfileRequest(
        @ValidLanguageCode(allowBcp47 = false) String preferredLanguage,
        @Size(max = 5) String preferredCurrency
) { }