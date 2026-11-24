package com.tradebridge.identity.event.payload;

import java.time.Instant;

/**
 * {@code identity.user.password.changed.v1} payload.
 */
public record PasswordChangedPayload(
        String userId,
        Instant changedAt
) { }