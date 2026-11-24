package com.tradebridge.identity.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Payload for PATCH /api/v1/identity/admin/users/{userId}/status.
 */
public record UpdateUserStatusRequest(
        @NotBlank @Pattern(regexp = "^(ACTIVE|SUSPENDED|LOCKED|CLOSED)$",
                message = "Must be ACTIVE, SUSPENDED, LOCKED or CLOSED")
        String status,
        String reason
) { }