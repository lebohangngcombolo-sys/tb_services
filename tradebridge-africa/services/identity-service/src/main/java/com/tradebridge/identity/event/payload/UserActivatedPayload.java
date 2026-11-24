package com.tradebridge.identity.event.payload;

import java.time.Instant;

/**
 * {@code identity.user.activated.v1} payload.
 */
public record UserActivatedPayload(
        String userId,
        Instant activatedAt
) { }