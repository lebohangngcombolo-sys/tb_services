package com.tradebridge.identity.api.dto.request;

import com.tradebridge.validation.annotation.ValidEmail;
import jakarta.validation.constraints.NotBlank;

/**
 * Payload for POST /api/v1/identity/password/forgot.
 */
public record ForgotPasswordRequest(@ValidEmail String email) { }