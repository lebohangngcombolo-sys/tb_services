package com.tradebridge.customer.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Status change audit (spec §6.2 {@code customer_status_history}).
 *
 * <p>Either {@code customerId} or {@code organizationId} is set, not both.
 */
@Entity
@Table(name = "customer_status_history")
public class CustomerStatusHistory {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "customer_id")
    private UUID customerId;

    @Column(name = "organization_id")
    private UUID organizationId;

    @Column(name = "old_status", length = 30)
    private String oldStatus;

    @Column(name = "new_status", nullable = false, length = 30)
    private String newStatus;

    @Column(name = "reason", columnDefinition = "text")
    private String reason;

    @Column(name = "changed_by_user_id")
    private UUID changedByUserId;

    @Column(name = "changed_at", nullable = false, updatable = false)
    private Instant changedAt;

    protected CustomerStatusHistory() { }

    public CustomerStatusHistory(UUID id, UUID customerId, UUID organizationId,
                                  String oldStatus, String newStatus, String reason,
                                  UUID changedByUserId) {
        this.id = Objects.requireNonNull(id, "id");
        this.customerId = customerId;
        this.organizationId = organizationId;
        this.oldStatus = oldStatus;
        this.newStatus = Objects.requireNonNull(newStatus, "newStatus");
        this.reason = reason;
        this.changedByUserId = changedByUserId;
    }

    @PrePersist
    void onCreate() {
        if (changedAt == null) changedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getCustomerId() { return customerId; }
    public UUID getOrganizationId() { return organizationId; }
    public String getOldStatus() { return oldStatus; }
    public String getNewStatus() { return newStatus; }
    public String getReason() { return reason; }
    public UUID getChangedByUserId() { return changedByUserId; }
    public Instant getChangedAt() { return changedAt; }
}