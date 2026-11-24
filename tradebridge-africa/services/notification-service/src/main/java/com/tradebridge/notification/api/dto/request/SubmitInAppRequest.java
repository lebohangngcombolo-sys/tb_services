package com.tradebridge.notification.api.dto.request;

import com.tradebridge.common.enums.Priority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record SubmitInAppRequest(
        @NotBlank @Size(max = 100) String templateCode,
        @NotBlank @Size(max = 5) String languageCode,
        @NotNull UUID recipientUserId,
        @NotNull Map<String, Object> variables,
        Priority priority,
        @Size(max = 150) String idempotencyKey,
        Instant expiresAt
) { }