package com.tradebridge.identity.event.payload;

/**
 * {@code identity.user.mfa.enabled.v1} payload.
 */
public record MfaEnabledPayload(
        String userId,
        String factorType
) { }