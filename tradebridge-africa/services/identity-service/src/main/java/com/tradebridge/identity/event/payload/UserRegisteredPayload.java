package com.tradebridge.identity.event.payload;

/**
 * {@code identity.user.registered.v1} payload.
 *
 * <p>Minimum required by spec §3.3: userId, email, countryCode.
 * Deliberately excludes PII beyond what consumers need.
 */
public record UserRegisteredPayload(
        String userId,
        String email,
        String countryCode
) { }