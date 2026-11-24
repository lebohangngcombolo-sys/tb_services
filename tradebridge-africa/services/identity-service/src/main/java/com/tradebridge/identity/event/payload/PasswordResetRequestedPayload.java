package com.tradebridge.identity.event.payload;

/**
 * {@code identity.user.password.reset.requested.v1} payload.
 */
public record PasswordResetRequestedPayload(
        String userId,
        String challengeId,
        String destinationMasked
) { }