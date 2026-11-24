package com.tradebridge.identity.domain;

import com.tradebridge.messaging.inbox.InboxEntry;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Concrete inbox entity for the identity service.
 *
 * <p>Table name {@code identity_inbox} is created by
 * {@code V3__outbox_inbox.sql}. The messaging starter's {@code InboxGuard}
 * uses this class via {@link com.tradebridge.identity.config.InboxProvider}.
 */
@Entity
@Table(name = "identity_inbox")
public class IdentityInboxEntry extends InboxEntry {
}
