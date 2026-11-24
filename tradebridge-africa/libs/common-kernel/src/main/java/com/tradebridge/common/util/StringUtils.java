package com.tradebridge.common.util;

import java.util.Collection;
import java.util.Locale;
import java.util.Objects;

/**
 * Small, dependency-free helpers. Validation-specific string rules
 * (email, phone, country codes) belong in {@code validation-starter}.
 */
public final class StringUtils {

    private StringUtils() { }

    public static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public static boolean isNotBlank(String value) {
        return !isBlank(value);
    }

    public static String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public static String normalizeLower(String value) {
        String trimmed = trimToNull(value);
        return trimmed == null ? null : trimmed.toLowerCase(Locale.ROOT);
    }

    public static String normalizeUpper(String value) {
        String trimmed = trimToNull(value);
        return trimmed == null ? null : trimmed.toUpperCase(Locale.ROOT);
    }

    public static String requireNonBlank(String value, String fieldName) {
        if (isBlank(value)) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }

    public static String truncate(String value, int maxLength) {
        if (value == null) return null;
        if (maxLength < 0) throw new IllegalArgumentException("maxLength must be >= 0");
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    public static String join(Collection<?> parts, String delimiter) {
        Objects.requireNonNull(parts, "parts");
        Objects.requireNonNull(delimiter, "delimiter");
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Object p : parts) {
            if (p == null) continue;
            if (!first) sb.append(delimiter);
            sb.append(p);
            first = false;
        }
        return sb.toString();
    }

    /**
     * Masks an email for display: {@code jo***@example.com}.
     * Used in logs and notification recipient fields. Never store or log
     * plaintext destination addresses in notification history (§5.4).
     */
    public static String maskEmail(String email) {
        if (isBlank(email)) return null;
        int at = email.indexOf('@');
        if (at <= 0 || at == email.length() - 1) {
            return "***";
        }
        String local = email.substring(0, at);
        String domain = email.substring(at);
        String head = local.length() <= 2 ? local.charAt(0) + "" : local.substring(0, 2);
        return head + "***" + domain;
    }

    /**
     * Masks an E.164 phone for display: {@code +27*****6789}.
     */
    public static String maskPhone(String phone) {
        if (isBlank(phone)) return null;
        String trimmed = phone.trim();
        if (trimmed.length() <= 4) {
            return "****";
        }
        int prefixLen = trimmed.startsWith("+") ? 3 : 2;
        if (trimmed.length() <= prefixLen + 4) {
            return "****";
        }
        String prefix = trimmed.substring(0, Math.min(prefixLen, trimmed.length()));
        String suffix = trimmed.substring(trimmed.length() - 4);
        return prefix + "****" + suffix;
    }
}