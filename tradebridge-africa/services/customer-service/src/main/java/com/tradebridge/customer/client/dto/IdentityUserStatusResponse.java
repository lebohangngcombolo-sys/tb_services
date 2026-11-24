package com.tradebridge.customer.client.dto;

import java.util.UUID;

/**
 * Subset of {@code GET /api/v1/identity/admin/users/{userId}} needed by
 * customer-service to verify a representative's account status.
 */
public record IdentityUserStatusResponse(
        UUID id,
        String email,
        String status,
        boolean emailVerified
) { }