package com.tradebridge.identity.api.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Payload for POST /api/v1/identity/mfa/setup/totp.
 * Password re-authentication is required to begin MFA setup.
 */
public record MfaSetupRequest(@NotBlank String currentPassword) { }