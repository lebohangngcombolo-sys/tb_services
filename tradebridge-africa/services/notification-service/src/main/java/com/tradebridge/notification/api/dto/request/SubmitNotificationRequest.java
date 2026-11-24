package com.tradebridge.notification.api.dto.request;

import com.tradebridge.common.enums.Channel;
import com.tradebridge.common.enums.Priority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Generic submission (spec §5.1A). Resolves channel-specific rules in
 * the service layer.
 */
public record SubmitNotificationRequest(
        @NotNull Channel channel,
        @NotBlank @Size(max = 100) String templateCode,
        @NotBlank @Size(max = 5) String languageCode,
        @NotBlank @Size(max = 255) String recipient,
        @NotNull Map<String, Object> variables,
        Priority priority,
        UUID recipientUserId,
        UUID sourceServiceId,
        @Size(max = 150) String idempotencyKey,
        Instant scheduledAt,
        Instant expiresAt
) { }