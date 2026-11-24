package com.tradebridge.identity.event.payload;

/**
 * {@code identity.user.status.changed.v1} payload.
 */
public record StatusChangedPayload(
        String userId,
        String oldStatus,
        String newStatus
) { }