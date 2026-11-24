package com.tradebridge.notification.domain;

import com.tradebridge.messaging.inbox.InboxEntry;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Concrete inbox entity for idempotent consumption (spec §1.4).
 * Table {@code notification_inbox} created by {@code V3__outbox_inbox.sql}.
 */
@Entity
@Table(name = "notification_inbox")
public class NotificationInboxEntry extends InboxEntry {
}