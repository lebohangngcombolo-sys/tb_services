package com.tradebridge.identity.api.dto.response;

import java.util.UUID;

/**
 * Response for POST /api/v1/identity/register.
 */
public record RegistrationResponse(
        UUID userId,
        String email,
        String status,
        boolean emailVerificationRequired
) { }