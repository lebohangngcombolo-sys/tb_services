package com.tradebridge.identity.api.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Payload for POST /api/v1/identity/refresh.
 */
public record RefreshTokenRequest(@NotBlank String refreshToken) { }