package com.tradebridge.country.api.dto.request;

import com.tradebridge.validation.annotation.ValidCurrencyCode;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AddCurrencyRequest(
        @NotNull @ValidCurrencyCode String currencyCode,
        @Size(max = 100) String name,
        @Size(max = 10) String symbol,
        @NotNull Integer decimalPlaces,
        @NotNull Boolean isDefault
) { }