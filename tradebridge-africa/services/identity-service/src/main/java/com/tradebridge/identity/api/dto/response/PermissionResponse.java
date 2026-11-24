package com.tradebridge.identity.api.dto.response;

import java.util.UUID;

/**
 * Public-safe view of a permission.
 */
public record PermissionResponse(
        UUID id,
        String code,
        String resource,
        String action,
        String description
) { }