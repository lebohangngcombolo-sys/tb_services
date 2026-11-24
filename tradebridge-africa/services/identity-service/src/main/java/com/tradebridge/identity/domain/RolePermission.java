package com.tradebridge.identity.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.util.Objects;
import java.util.UUID;

/**
 * Role ↔ Permission mapping (spec §3.2 {@code role_permissions}).
 *
 * <p>Seeded via Flyway (V2 migration). Not exposed to end users.
 */
@Entity
@Table(name = "role_permissions",
        uniqueConstraints = @UniqueConstraint(name = "uk_role_permissions",
                columnNames = {"role_id", "permission_id"}))
public class RolePermission {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "role_id", nullable = false)
    private UUID roleId;

    @Column(name = "permission_id", nullable = false)
    private UUID permissionId;

    protected RolePermission() { }

    public RolePermission(UUID roleId, UUID permissionId) {
        this.id = UUID.randomUUID();
        this.roleId = Objects.requireNonNull(roleId, "roleId");
        this.permissionId = Objects.requireNonNull(permissionId, "permissionId");
    }

    public UUID getId() { return id; }
    public UUID getRoleId() { return roleId; }
    public UUID getPermissionId() { return permissionId; }
}
