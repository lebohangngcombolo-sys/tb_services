package com.tradebridge.identity.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Payload for TOTP / email OTP / recovery code verification endpoints.
 * Accepts either 6-digit TOTP/OTP or a recovery code string.
 */
public record MfaVerifyRequest(
        @NotBlank @Pattern(regexp = "^([0-9]{6}|[A-Z0-9]{8}-[A-Z0-9]{8})$",
                message = "Must be a 6-digit code or an XXXXXXXX-XXXXXXXX recovery code")
        String code
) { }