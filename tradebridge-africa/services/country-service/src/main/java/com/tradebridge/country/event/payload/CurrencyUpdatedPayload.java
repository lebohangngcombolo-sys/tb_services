package com.tradebridge.country.event.payload;

/**
 * {@code country.currency.updated.v1} payload.
 */
public record CurrencyUpdatedPayload(
        String countryCode,
        String currencyCode
) { }