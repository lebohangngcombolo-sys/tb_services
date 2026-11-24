package com.tradebridge.identity.mapper;

import com.tradebridge.identity.api.dto.response.RoleResponse;
import com.tradebridge.identity.domain.Role;

public final class RoleMapper {

    public RoleResponse toResponse(Role role) {
        if (role == null) return null;
        return new RoleResponse(role.getId(), role.getCode(), role.getName(),
                role.getDescription(), role.isSystemRole());
    }
}
