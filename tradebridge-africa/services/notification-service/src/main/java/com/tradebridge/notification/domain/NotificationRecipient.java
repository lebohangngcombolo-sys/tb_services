package com.tradebridge.notification.domain;

import com.tradebridge.notification.domain.enums.DestinationType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Recipient destination (spec §5.2 {@code notification_recipients}).
 *
 * <p>Stores the <b>masked</b> destination and a hash of the raw destination.
 * Raw destinations are only kept by the delivery pipeline for the duration
 * of a send; masked values serve operators and audit trail (spec §5.4
 * "Do not use this table to expose full contact details").
 */
@Entity
@Table(name = "notification_recipients")
public class NotificationRecipient {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "notification_id", nullable = false, updatable = false)
    private UUID notificationId;

    @Column(name = "user_id")
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "destination_type", nullable = false, length = 20)
    private DestinationType destinationType;

    @Column(name = "destination_masked", length = 255)
    private String destinationMasked;

    @Column(name = "destination_hash", length = 255)
    private String destinationHash;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected NotificationRecipient() { }

    public NotificationRecipient(UUID id, UUID notificationId, UUID userId,
                                 DestinationType destinationType, String destinationMasked,
                                 String destinationHash) {
        this.id = Objects.requireNonNull(id, "id");
        this.notificationId = Objects.requireNonNull(notificationId, "notificationId");
        this.userId = userId;
        this.destinationType = Objects.requireNonNull(destinationType, "destinationType");
        this.destinationMasked = destinationMasked;
        this.destinationHash = destinationHash;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getNotificationId() { return notificationId; }
    public UUID getUserId() { return userId; }
    public DestinationType getDestinationType() { return destinationType; }
    public String getDestinationMasked() { return destinationMasked; }
    public String getDestinationHash() { return destinationHash; }
    public Instant getCreatedAt() { return createdAt; }
}