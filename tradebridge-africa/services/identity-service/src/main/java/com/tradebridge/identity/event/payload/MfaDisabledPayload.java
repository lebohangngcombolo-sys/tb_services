package com.tradebridge.identity.event.payload;

/**
 * {@code identity.user.mfa.disabled.v1} payload.
 */
public record MfaDisabledPayload(
        String userId,
        String factorType
) { }