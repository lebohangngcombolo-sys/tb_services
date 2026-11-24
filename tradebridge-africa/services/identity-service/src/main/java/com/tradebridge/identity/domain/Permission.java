package com.tradebridge.identity.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.util.Objects;
import java.util.UUID;

/**
 * Permission aggregate (spec §3.2 {@code permissions}).
 *
 * <p>Permissions are seeded via Flyway (V2 migration) and referenced via
 * {@code PermissionCode} constants from {@code security-starter}.
 */
@Entity
@Table(name = "permissions",
        uniqueConstraints = @UniqueConstraint(name = "uk_permissions_code", columnNames = "code"))
public class Permission {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "code", nullable = false, length = 100)
    private String code;

    @Column(name = "resource", nullable = false, length = 100)
    private String resource;

    @Column(name = "action", nullable = false, length = 50)
    private String action;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    protected Permission() { }

    public Permission(String code, String resource, String action, String description) {
        this.id = UUID.randomUUID();
        this.code = Objects.requireNonNull(code, "code");
        this.resource = Objects.requireNonNull(resource, "resource");
        this.action = Objects.requireNonNull(action, "action");
        this.description = description;
    }

    public UUID getId() { return id; }
    public String getCode() { return code; }
    public String getResource() { return resource; }
    public String getAction() { return action; }
    public String getDescription() { return description; }
}
