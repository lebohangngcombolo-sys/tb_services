package com.tradebridge.validation.language;

import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Language code validation. Supports:
 * <ul>
 *   <li><b>ISO 639-1</b> two-letter language codes ({@code "en"}, {@code "pt"}).</li>
 *   <li><b>BCP 47</b> language tags with region subtag ({@code "en-ZA"}, {@code "pt-BR"}).</li>
 * </ul>
 *
 * <p>Spec §4.2 seeds {@code EN} and {@code PT} as initial languages.
 *
 * <p>Whether the language is <b>supported</b> in a country is a Country
 * Service concern (spec §4.4).
 */
public final class LanguageCodeValidator {

    /** ISO 639-1 two-letter language codes. */
    private static final Set<String> ISO_639_1 = Stream.of(Locale.getISOLanguages())
            .map(String::toLowerCase)
            .collect(Collectors.toUnmodifiableSet());

    private LanguageCodeValidator() { }

    /** True if {@code code} is an ISO 639-1 two-letter code. */
    public static boolean isValidIso639_1(String code) {
        if (code == null) return false;
        String lower = code.trim().toLowerCase(Locale.ROOT);
        return lower.length() == 2 && ISO_639_1.contains(lower);
    }

    /**
     * True if {@code tag} is a valid BCP 47 language tag whose primary
     * subtag is a valid ISO 639-1 code. Accepts optional region subtag.
     */
    public static boolean isValidBcp47(String tag) {
        if (tag == null) return false;
        String trimmed = tag.trim();
        if (trimmed.isEmpty()) return false;
        String[] parts = trimmed.split("-");
        if (parts.length < 1 || parts.length > 2) return false;
        if (!isValidIso639_1(parts[0])) return false;
        if (parts.length == 2) {
            return parts[1].matches("^[A-Za-z]{2}$") || parts[1].matches("^[0-9]{3}$");
        }
        return true;
    }

    /** True for either bare ISO 639-1 or full BCP 47. */
    public static boolean isValid(String code) {
        return isValidIso639_1(code) || isValidBcp47(code);
    }

    /**
     * Normalize: lowercase the language subtag, uppercase the region subtag
     * (e.g. {@code "EN-za"} → {@code "en-ZA"}).
     */
    public static String normalize(String code) {
        if (code == null) return null;
        String trimmed = code.trim();
        if (trimmed.isEmpty()) return trimmed;
        String[] parts = trimmed.split("-");
        if (parts.length == 1) {
            return parts[0].toLowerCase(Locale.ROOT);
        }
        StringBuilder sb = new StringBuilder(parts[0].toLowerCase(Locale.ROOT));
        for (int i = 1; i < parts.length; i++) {
            sb.append('-').append(parts[i].toUpperCase(Locale.ROOT));
        }
        return sb.toString();
    }
}
