package com.tradebridge.notification.provider.inapp;

import com.tradebridge.common.enums.Channel;
import com.tradebridge.notification.provider.NotificationProvider;
import com.tradebridge.notification.provider.ProviderResult;
import com.tradebridge.notification.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * In-app provider — "delivery" is instant once the notification is
 * persisted. This provider's job is purely to satisfy the uniform
 * delivery pipeline so {@code notification_deliveries} has an entry.
 */
@Component
public class DatabaseInAppProvider implements NotificationProvider {

    private static final Logger log = LoggerFactory.getLogger(DatabaseInAppProvider.class);
    public static final String NAME = "database-in-app";

    private final NotificationRepository notificationRepository;

    public DatabaseInAppProvider(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Override public String name() { return NAME; }
    @Override public Channel channel() { return Channel.IN_APP; }

    @Override
    public boolean isEnabled() { return true; }

    @Override
    public ProviderResult send(String destination, String subject, String body) {
        // Destination here is a userId (string form). Because the notification
        // row was persisted by the submission service, "delivery" is a no-op.
        // We still record a provider message ID for the audit trail.
        String messageId = "inapp:" + UUID.randomUUID();
        log.debug("In-app notification persisted for user {}", destination);
        return ProviderResult.accepted(NAME, messageId);
    }
}