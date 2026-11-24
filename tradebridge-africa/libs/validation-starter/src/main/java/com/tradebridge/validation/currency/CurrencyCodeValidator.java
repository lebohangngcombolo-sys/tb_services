package com.tradebridge.validation.currency;

import java.util.Currency;
import java.util.Locale;
import java.util.Set;

/**
 * ISO 4217 currency code validation.
 *
 * <p>Uses {@link Currency#getAvailableCurrencies()} from the JDK — this is
 * the authoritative ISO 4217 set at the JVM's build time. Codes are
 * 3 uppercase letters.
 *
 * <p>Whether the currency is <b>enabled</b> for a country is a Country
 * Service concern (spec §4.4).
 */
public final class CurrencyCodeValidator {

    private static final Set<String> ISO_CODES = buildIsoSet();

    private CurrencyCodeValidator() { }

    public static boolean isValid(String code) {
        if (code == null) return false;
        String upper = code.trim().toUpperCase(Locale.ROOT);
        return upper.length() == 3 && ISO_CODES.contains(upper);
    }

    public static String normalize(String code) {
        if (code == null) return null;
        return code.trim().toUpperCase(Locale.ROOT);
    }

    private static Set<String> buildIsoSet() {
        return Currency.getAvailableCurrencies().stream()
                .map(Currency::getCurrencyCode)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }
}
