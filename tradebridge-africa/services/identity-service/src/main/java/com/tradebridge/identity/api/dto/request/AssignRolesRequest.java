package com.tradebridge.identity.api.dto.request;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * Payload for PUT /api/v1/identity/admin/users/{userId}/roles.
 */
public record AssignRolesRequest(@NotEmpty List<String> roleCodes) { }