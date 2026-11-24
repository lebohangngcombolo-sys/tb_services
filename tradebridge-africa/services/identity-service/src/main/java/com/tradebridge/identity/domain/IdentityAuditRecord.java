package com.tradebridge.identity.domain;

import com.tradebridge.identity.domain.enums.IdentityAction;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.net.InetAddress;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Identity-specific security audit record (spec §3.2
 * {@code identity_audit_records}).
 *
 * <p>Metadata must never contain passwords, tokens, OTPs, or KYC content
 * (spec §2.5). Metadata is stored as {@code JSONB} for query flexibility.
 */
@Entity
@Table(name = "identity_audit_records")
public class IdentityAuditRecord {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "actor_id")
    private UUID actorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 100)
    private IdentityAction action;

    @JdbcTypeCode(SqlTypes.INET)
    @Column(name = "ip_address", columnDefinition = "inet")
    private InetAddress ipAddress;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    protected IdentityAuditRecord() { }

    public IdentityAuditRecord(UUID userId, UUID actorId, IdentityAction action,
                               String ipAddress, Map<String, Object> metadata) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.actorId = actorId;
        this.action = Objects.requireNonNull(action, "action");
        this.ipAddress = IpAddressParser.parse(ipAddress);
        this.metadata = metadata;
    }

    @PrePersist
    void onCreate() {
        if (occurredAt == null) occurredAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public UUID getActorId() { return actorId; }
    public IdentityAction getAction() { return action; }
    public String getIpAddress() { return ipAddress == null ? null : ipAddress.getHostAddress(); }
    public Map<String, Object> getMetadata() { return metadata; }
    public Instant getOccurredAt() { return occurredAt; }
}
