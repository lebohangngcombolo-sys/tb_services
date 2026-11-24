package com.tradebridge.identity.api.dto.request;

import com.tradebridge.validation.annotation.ValidEmail;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Payload for POST /api/v1/identity/verify-email.
 */
public record VerifyEmailRequest(
        @ValidEmail String email,
        @NotBlank @Pattern(regexp = "^\\d{6}$", message = "OTP must be 6 digits") String code
) { }