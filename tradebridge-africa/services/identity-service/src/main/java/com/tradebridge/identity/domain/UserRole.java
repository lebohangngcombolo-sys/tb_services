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
 * User ↔ Role assignment (spec §3.2 {@code user_roles}).
 *
 * <p>Assignments can be time-bounded ({@code expires_at}) and record the
 * assigning actor for audit purposes.
 */
@Entity
@Table(name = "user_roles",
        uniqueConstraints = @UniqueConstraint(name = "uk_user_roles", columnNames = {"user_id", "role_id"}))
public class UserRole {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "role_id", nullable = false)
    private UUID roleId;

    @Column(name = "assigned_by")
    private UUID assignedBy;

    @Column(name = "assigned_at", nullable = false)
    private Instant assignedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    protected UserRole() { }

    public UserRole(UUID userId, UUID roleId, UUID assignedBy, Instant expiresAt) {
        this.id = UUID.randomUUID();
        this.userId = Objects.requireNonNull(userId, "userId");
        this.roleId = Objects.requireNonNull(roleId, "roleId");
        this.assignedBy = assignedBy;
        this.expiresAt = expiresAt;
    }

    @PrePersist
    void onCreate() {
        if (assignedAt == null) assignedAt = Instant.now();
    }

    public boolean isActiveAt(Instant when) {
        return expiresAt == null || when.isBefore(expiresAt);
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public UUID getRoleId() { return roleId; }
    public UUID getAssignedBy() { return assignedBy; }
    public Instant getAssignedAt() { return assignedAt; }
    public Instant getExpiresAt() { return expiresAt; }
}
