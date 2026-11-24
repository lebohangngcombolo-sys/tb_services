package com.tradebridge.notification.api.dto.request;

import com.tradebridge.common.enums.Priority;
import com.tradebridge.validation.annotation.ValidEmail;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record SubmitEmailRequest(
        @NotBlank @Size(max = 100) String templateCode,
        @NotBlank @Size(max = 5) String languageCode,
        @ValidEmail String recipient,
        @NotNull Map<String, Object> variables,
        Priority priority,
        UUID recipientUserId,
        @Size(max = 150) String idempotencyKey,
        Instant scheduledAt,
        Instant expiresAt
) { }