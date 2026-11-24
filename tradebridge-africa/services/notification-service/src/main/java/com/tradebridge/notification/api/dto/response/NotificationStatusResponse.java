package com.tradebridge.notification.api.dto.response;

import java.time.Instant;
import java.util.UUID;

/**
 * Submission confirmation (spec §5.1A).
 */
public record NotificationStatusResponse(
        UUID id,
        String channel,
        String status,
        String templateCode,
        Instant createdAt,
        Instant scheduledAt
) { }