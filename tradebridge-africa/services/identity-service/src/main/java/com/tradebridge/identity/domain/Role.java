package com.tradebridge.identity.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Role aggregate (spec §3.2 {@code roles}).
 *
 * <p>Roles are seeded via Flyway (V2 migration) and referenced throughout
 * the codebase via {@code PermissionCode} constants.
 */
@Entity
@Table(name = "roles",
        uniqueConstraints = @UniqueConstraint(name = "uk_roles_code", columnNames = "code"))
public class Role {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "system_role", nullable = false)
    private boolean systemRole;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Role() { }

    public Role(String code, String name, String description, boolean systemRole) {
        this.id = UUID.randomUUID();
        this.code = Objects.requireNonNull(code, "code");
        this.name = Objects.requireNonNull(name, "name");
        this.description = description;
        this.systemRole = systemRole;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public boolean isSystemRole() { return systemRole; }
    public Instant getCreatedAt() { return createdAt; }

    public void updateName(String name, String description) {
        this.name = Objects.requireNonNull(name);
        this.description = description;
    }
}
