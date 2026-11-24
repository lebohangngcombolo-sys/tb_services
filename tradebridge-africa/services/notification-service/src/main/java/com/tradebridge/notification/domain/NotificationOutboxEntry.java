package com.tradebridge.notification.domain;

import com.tradebridge.messaging.outbox.OutboxEntry;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Concrete outbox entity for the notification service (spec §1.4).
 * Table {@code notification_outbox} created by {@code V3__outbox_inbox.sql}.
 */
@Entity
@Table(name = "notification_outbox")
public class NotificationOutboxEntry extends OutboxEntry {
}