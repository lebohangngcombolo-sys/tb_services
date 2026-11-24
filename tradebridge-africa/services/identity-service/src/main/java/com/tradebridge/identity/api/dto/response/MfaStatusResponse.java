package com.tradebridge.identity.api.dto.response;

import java.time.Instant;

/**
 * Response for GET /api/v1/identity/mfa/status.
 */
public record MfaStatusResponse(
        boolean enabled,
        String factorType,
        Instant enabledAt,
        int remainingRecoveryCodes
) { }