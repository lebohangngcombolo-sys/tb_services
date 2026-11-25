package com.tradebridge.identity.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Completes login MFA using the short-lived challenge returned by login.
 */
public record MfaLoginVerifyRequest(
        @NotBlank
        @Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$",
                message = "Must be a valid MFA challenge identifier")
        String challengeId,
        @NotBlank
        @Pattern(regexp = "^([0-9]{6}|[A-Z0-9]{8}-[A-Z0-9]{8})$",
                message = "Must be a 6-digit code or an XXXXXXXX-XXXXXXXX recovery code")
        String code
) { }
