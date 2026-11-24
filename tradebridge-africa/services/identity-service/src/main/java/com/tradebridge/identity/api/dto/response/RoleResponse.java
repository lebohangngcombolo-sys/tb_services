package com.tradebridge.identity.api.dto.response;

import java.util.UUID;

/**
 * Public-safe view of a role.
 */
public record RoleResponse(
        UUID id,
        String code,
        String name,
        String description,
        boolean systemRole
) { }