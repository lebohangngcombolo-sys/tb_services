package com.tradebridge.validation.email;

import java.util.Locale;
import java.util.Objects;

/**
 * Value object carrying an email in canonical form for storage/comparison.
 *
 * <p>Normalization rules:
 * <ul>
 *   <li>Trim whitespace.</li>
 *   <li>Lowercase the entire address (spec §3.4: "normalized to lowercase").</li>
 * </ul>
 *
 * <p>The local-part case-sensitivity debate is well known; the spec is
 * explicit about lowercase normalization, and we follow it everywhere
 * (registration, login, password reset) for consistency.
 */
public final class NormalizedEmail {

    private final String value;

    private NormalizedEmail(String value) {
        this.value = value;
    }

    public static NormalizedEmail of(String raw) {
        if (raw == null) {
            throw new IllegalArgumentException("email must not be null");
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("email must not be blank");
        }
        return new NormalizedEmail(trimmed.toLowerCase(Locale.ROOT));
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NormalizedEmail that)) return false;
        return value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
