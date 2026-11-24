package com.tradebridge.notification.api.dto.response;

import java.time.Instant;
import java.util.UUID;

/**
 * Customer-facing notification (spec §5.1B). Never includes raw message
 * bodies for security-sensitive channels.
 */
public record NotificationResponse(
        UUID id,
        String channel,
        String subject,
        String bodyPreview,
        String status,
        String priority,
        boolean read,
        Instant createdAt,
        Instant readAt
) { }