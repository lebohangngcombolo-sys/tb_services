package com.tradebridge.country.mapper;

import com.tradebridge.country.api.dto.response.CurrencyResponse;
import com.tradebridge.country.domain.Currency;

public final class CurrencyMapper {

    public CurrencyResponse toResponse(Currency currency, boolean isDefault) {
        if (currency == null) return null;
        return new CurrencyResponse(
                currency.getCode(),
                currency.getName(),
                currency.getSymbol(),
                currency.getDecimalPlaces(),
                currency.isEnabled(),
                isDefault);
    }
}