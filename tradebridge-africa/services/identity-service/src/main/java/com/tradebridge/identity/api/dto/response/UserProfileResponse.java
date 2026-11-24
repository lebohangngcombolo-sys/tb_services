package com.tradebridge.identity.api.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Public-safe view of a user (no password hashes, no secrets).
 */
public record UserProfileResponse(
        UUID id,
        String firstName,
        String lastName,
        String email,
        String phoneNumber,
        String countryCode,
        String preferredLanguage,
        String status,
        boolean emailVerified,
        boolean phoneVerified,
        boolean mfaEnabled,
        List<String> roles,
        Instant createdAt,
        Instant updatedAt
) { }