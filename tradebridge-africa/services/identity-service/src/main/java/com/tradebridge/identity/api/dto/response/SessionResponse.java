package com.tradebridge.identity.api.dto.response;

import java.time.Instant;
import java.util.UUID;

/**
 * Description of one active session. Never exposes tokens.
 */
public record SessionResponse(
        UUID sessionId,
        String deviceName,
        String ipAddress,
        Instant createdAt,
        Instant lastActivityAt,
        Instant expiresAt,
        boolean current
) { }