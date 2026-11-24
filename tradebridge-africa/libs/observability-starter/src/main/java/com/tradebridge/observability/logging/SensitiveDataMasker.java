package com.tradebridge.observability.logging;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Redacts sensitive data from log events before serialization.
 *
 * <p>Rules per spec §2.5, §3.4, §5.4, §7.4:
 * <ul>
 *   <li>Never log passwords, tokens, OTPs, or KYC content.</li>
 *   <li>Mask sensitive-shaped strings wherever they appear in the log message.</li>
 *   <li>Drop sensitive-keyed map entries entirely (do not mask — drop).</li>
 * </ul>
 *
 * <p>This masker is called from {@link StructuredJsonLayout} on every event.
 * It is a safety net, not a license to log secrets — service code must
 * still never pass them.
 */
public final class SensitiveDataMasker {

    /** JSON/JSONP keys that must never appear in a log. */
    private static final List<String> DROP_KEYS = List.of(
            "password",
            "currentPassword",
            "newPassword",
            "confirmPassword",
            "passwordHash",
            "password_hash",
            "token",
            "accessToken",
            "access_token",
            "refreshToken",
            "refresh_token",
            "tokenHash",
            "token_hash",
            "authorization",
            "cookie",
            "set-cookie",
            "otp",
            "otpCode",
            "otp_code",
            "codeHash",
            "code_hash",
            "secret",
            "secretKey",
            "apiKey",
            "api_key",
            "privateKey",
            "private_key",
            "totpSecret",
            "recoveryCode",
            "recovery_code",
            "kycDocument",
            "kyc_document",
            "documentBody",
            "document_body",
            "ssn",
            "taxId",
            "tax_id"
    );

    /** Bearer-like tokens: any sequence starting with "Bearer " or a JWT. */
    private static final Pattern BEARER_PATTERN = Pattern.compile(
            "(?i)bearer\\s+[A-Za-z0-9._~+\\-/=]+");

    /** JWT shape — three base64url segments separated by dots. */
    private static final Pattern JWT_PATTERN = Pattern.compile(
            "eyJ[A-Za-z0-9_-]{10,}\\.[A-Za-z0-9_-]{10,}\\.[A-Za-z0-9_-]{10,}");

    /** E.164-looking phone numbers (kept only in log-visible fields). */
    private static final Pattern E164_PATTERN = Pattern.compile(
            "\\+[1-9]\\d{6,14}");

    /** Email addresses. */
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");

    private SensitiveDataMasker() { }

    /**
     * Mask the given free-form message. Safe for null.
     */
    public static String maskMessage(String message) {
        if (message == null || message.isEmpty()) {
            return message;
        }
        String masked = BEARER_PATTERN.matcher(message).replaceAll("Bearer ***");
        masked = JWT_PATTERN.matcher(masked).replaceAll("***JWT***");
        masked = EMAIL_PATTERN.matcher(masked).replaceAll(match -> {
            String addr = match.group();
            int at = addr.indexOf('@');
            if (at <= 0) return "***";
            return maskLocal(addr.substring(0, at)) + addr.substring(at);
        });
        masked = E164_PATTERN.matcher(masked).replaceAll("+***MASKED***");
        return masked;
    }

    /**
     * Return a copy of the map with sensitive keys removed and remaining
     * string values masked. Null-safe. Preserves iteration order.
     */
    public static Map<String, Object> maskMap(Map<String, Object> input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        Map<String, Object> out = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : input.entrySet()) {
            String key = entry.getKey();
            if (key == null) {
                continue;
            }
            if (isDropKey(key)) {
                continue;
            }
            Object value = entry.getValue();
            if (value instanceof String s) {
                out.put(key, maskMessage(s));
            } else if (value instanceof Map<?, ?> nested) {
                @SuppressWarnings("unchecked")
                Map<String, Object> cast = (Map<String, Object>) nested;
                out.put(key, maskMap(cast));
            } else {
                out.put(key, value);
            }
        }
        return out;
    }

    /** Return true if the field value must be entirely dropped from logs. */
    public static boolean isDropKey(String key) {
        if (key == null || key.isEmpty()) {
            return false;
        }
        String lower = key.toLowerCase();
        for (String candidate : DROP_KEYS) {
            if (lower.equals(candidate.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private static String maskLocal(String local) {
        if (local.length() <= 2) return "***";
        return local.substring(0, 2) + "***";
    }

    /**
     * Test-only helper: returns the list of sensitive keys this masker
     * drops. Keeps the masker internally consistent if someone adds a
     * key without updating the masker.
     */
    static List<String> dropKeySnapshot() {
        return List.copyOf(DROP_KEYS);
    }
}
