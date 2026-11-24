package com.tradebridge.identity.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.net.InetAddress;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Active login session (spec §3.2 {@code user_sessions}).
 *
 * <p>The session ID is included in every access token as the {@code sid}
 * claim so that sessions can be revoked without waiting for JWT expiry
 * (spec §3.4 "Role changes should ... invalidate or refresh authorization
 * state for existing sessions").
 */
@Entity
@Table(name = "user_sessions")
public class UserSession {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "device_name", length = 150)
    private String deviceName;

    @JdbcTypeCode(SqlTypes.INET)
    @Column(name = "ip_address", columnDefinition = "inet")
    private InetAddress ipAddress;

    @Column(name = "user_agent", columnDefinition = "text")
    private String userAgent;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "last_activity_at")
    private Instant lastActivityAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    protected UserSession() { }

    public UserSession(UUID id, UUID userId, String deviceName, String ipAddress,
                       String userAgent, Instant expiresAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.userId = Objects.requireNonNull(userId, "userId");
        this.deviceName = deviceName;
        this.ipAddress = IpAddressParser.parse(ipAddress);
        this.userAgent = userAgent;
        this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt");
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        if (lastActivityAt == null) lastActivityAt = now;
    }

    public void touch() {
        this.lastActivityAt = Instant.now();
    }

    public void revoke() {
        if (revokedAt == null) {
            this.revokedAt = Instant.now();
        }
    }

    public boolean isActiveAt(Instant when) {
        return revokedAt == null && when.isBefore(expiresAt);
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getDeviceName() { return deviceName; }
    public String getIpAddress() { return ipAddress == null ? null : ipAddress.getHostAddress(); }
    public String getUserAgent() { return userAgent; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getLastActivityAt() { return lastActivityAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getRevokedAt() { return revokedAt; }
}
