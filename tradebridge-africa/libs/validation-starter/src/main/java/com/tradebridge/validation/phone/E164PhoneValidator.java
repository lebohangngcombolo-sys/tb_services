package com.tradebridge.validation.phone;

import java.util.regex.Pattern;

/**
 * E.164 international phone number format check.
 *
 * <p>E.164 structure: {@code +} followed by country calling code (1-3 digits)
 * followed by subscriber number. The full number is at most 15 digits and
 * at least 7 digits (practical minimum after country code).
 *
 * <p>Spec §3.4: "Phone number must be valid E.164 and unique."
 *
 * <p>This validator checks <b>format</b> only. Whether the country calling
 * code is enabled or the number belongs to a supported country is a
 * Country Service concern.
 */
public final class E164PhoneValidator {

    private static final int MIN_DIGITS = 7;
    private static final int MAX_DIGITS = 15;

    private static final Pattern E164_PATTERN = Pattern.compile("^\\+[1-9]\\d{6,14}$");

    private E164PhoneValidator() { }

    public static boolean isValid(String raw) {
        if (raw == null || raw.isBlank()) {
            return false;
        }
        String trimmed = raw.trim();
        if (!E164_PATTERN.matcher(trimmed).matches()) {
            return false;
        }
        int digitCount = trimmed.length() - 1; // exclude '+'
        return digitCount >= MIN_DIGITS && digitCount <= MAX_DIGITS;
    }

    /**
     * Normalize by stripping all non-digit characters except a leading '+',
     * then validating. Rejects numbers that lack a country code (no leading '+').
     */
    public static String normalize(String raw) {
        if (raw == null) {
            throw new IllegalArgumentException("phone number must not be null");
        }
        String trimmed = raw.trim();
        if (!trimmed.startsWith("+")) {
            throw new IllegalArgumentException("Phone number must include country code (start with '+')");
        }
        // Strip common formatting: spaces, dashes, parentheses, dots
        String digits = trimmed.replaceAll("[\\s\\-().]", "");
        if (!isValid(digits)) {
            throw new IllegalArgumentException("Invalid E.164 phone number");
        }
        return digits;
    }
}
