package com.tradebridge.identity.event.payload;

import java.util.List;

/**
 * {@code identity.user.role.changed.v1} payload.
 */
public record RoleChangedPayload(
        String userId,
        List<String> roleCodes
) { }