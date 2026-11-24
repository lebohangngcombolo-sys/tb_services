package com.tradebridge.notification.api.dto.request;

import com.tradebridge.common.enums.Channel;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Map;

/**
 * Bulk submission — a single template with per-recipient variables.
 * Batch size is enforced by configuration (§5.4).
 */
public record BulkSubmitRequest(
        @NotNull Channel channel,
        @NotBlank @Size(max = 100) String templateCode,
        @NotBlank @Size(max = 5) String languageCode,
        @NotEmpty List<@Valid Recipient> recipients
) {
    public record Recipient(
            @NotBlank @Size(max = 255) String destination,
            java.util.UUID recipientUserId,
            @NotNull Map<String, Object> variables,
            @Size(max = 150) String idempotencyKey
    ) { }
}