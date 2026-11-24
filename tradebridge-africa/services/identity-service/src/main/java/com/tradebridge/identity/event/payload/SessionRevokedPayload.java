package com.tradebridge.identity.event.payload;

/**
 * {@code identity.user.session.revoked.v1} payload.
 * {@code sessionId} is null when all sessions were revoked.
 */
public record SessionRevokedPayload(
        String userId,
        String sessionId
) { }