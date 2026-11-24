package com.tradebridge.identity.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Payload for POST /api/v1/identity/password/reset.
 */
public record ResetPasswordRequest(
        @NotBlank String token,
        @NotBlank @Size(min = 12, max = 128) String newPassword
) { }