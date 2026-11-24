package com.tradebridge.identity.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Payload for POST /api/v1/identity/login.
 *
 * <p>{@code identifier} may be an email (contains '@') or an E.164 phone
 * (starts with '+'). The service resolves the user accordingly.
 */
public record LoginRequest(
        @NotBlank @Size(min = 3, max = 254) String identifier,
        @NotBlank @Size(max = 128) String password,
        String deviceName,
        String userAgent
) { }