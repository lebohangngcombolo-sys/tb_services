package com.tradebridge.validation.email;

import java.util.regex.Pattern;

/**
 * RFC 5322-influenced email format check.
 *
 * <p>Deliberately pragmatic rather than RFC-complete: rejects obvious
 * garbage and enforces a strict local-part + domain-literal structure.
 * Spec §3.4: "Email must be syntactically valid and normalized to lowercase."
 *
 * <p>Uses a bounded length check (254 chars total, 64 local, 255 domain)
 * matching the practical maximums from RFC 5321 and RFC 1035.
 */
public final class EmailValidator {

    /** 254 = RFC 5321 maximum forward-path length. */
    private static final int MAX_EMAIL_LENGTH = 254;
    private static final int MAX_LOCAL_LENGTH = 64;
    private static final int MAX_DOMAIN_LENGTH = 255;

    /**
     * Deliberately permissive local part (allows +, ., _, -, '), disallows
     * dots at start/end and consecutive dots. Domain requires at least one dot
     * and a 2+ char TLD.
     */
    private static final Pattern LOCAL_PATTERN = Pattern.compile(
            "^(?!\\.)(?!.*\\.\\.)[A-Za-z0-9!#$%&'*+/=?^_`{|}~.-]{1,64}(?<!\\.)$");

    private static final Pattern DOMAIN_PATTERN = Pattern.compile(
            "^(?=.{1,255}$)(?:[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?\\.)+[A-Za-z]{2,63}$");

    private EmailValidator() { }

    /**
     * True if {@code raw} is a syntactically valid email (case-insensitive).
     */
    public static boolean isValid(String raw) {
        if (raw == null || raw.isBlank()) {
            return false;
        }
        String trimmed = raw.trim();
        if (trimmed.length() > MAX_EMAIL_LENGTH) {
            return false;
        }
        int at = trimmed.lastIndexOf('@');
        if (at <= 0 || at == trimmed.length() - 1) {
            return false;
        }
        String local = trimmed.substring(0, at);
        String domain = trimmed.substring(at + 1);
        if (local.length() > MAX_LOCAL_LENGTH || domain.length() > MAX_DOMAIN_LENGTH) {
            return false;
        }
        return LOCAL_PATTERN.matcher(local).matches()
                && DOMAIN_PATTERN.matcher(domain).matches();
    }

    /**
     * Normalize to lowercase + trim. Throws if invalid.
     */
    public static NormalizedEmail normalize(String raw) {
        if (!isValid(raw)) {
            throw new IllegalArgumentException("Invalid email address");
        }
        return NormalizedEmail.of(raw);
    }
}
