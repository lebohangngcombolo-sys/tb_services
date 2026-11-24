package com.tradebridge.notification.domain;

import com.tradebridge.common.enums.Channel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Channel preference per user (spec §5.2 {@code notification_preferences}).
 *
 * <p>Security-critical channels are always sent regardless of preference —
 * this is enforced by {@code MandatoryNotificationClassifier} in the
 * service layer (spec §5.4 "Security-critical messages ... must not be
 * disabled by ordinary marketing or notification preferences").
 */
@Entity
@Table(name = "notification_preferences",
        uniqueConstraints = @UniqueConstraint(name = "uk_notification_preferences",
                columnNames = {"user_id", "channel"}))
public class NotificationPreference {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 20)
    private Channel channel;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Column(name = "language_code", length = 5)
    private String languageCode;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected NotificationPreference() { }

    public NotificationPreference(UUID id, UUID userId, Channel channel,
                                   boolean enabled, String languageCode) {
        this.id = Objects.requireNonNull(id, "id");
        this.userId = Objects.requireNonNull(userId, "userId");
        this.channel = Objects.requireNonNull(channel, "channel");
        this.enabled = enabled;
        this.languageCode = languageCode;
    }

    @PrePersist
    void onCreate() {
        if (updatedAt == null) updatedAt = Instant.now();
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public void update(boolean enabled, String languageCode) {
        this.enabled = enabled;
        if (languageCode != null) this.languageCode = languageCode;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public Channel getChannel() { return channel; }
    public boolean isEnabled() { return enabled; }
    public String getLanguageCode() { return languageCode; }
    public Instant getUpdatedAt() { return updatedAt; }
}