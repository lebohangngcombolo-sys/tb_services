package com.tradebridge.notification.config;

import com.tradebridge.messaging.outbox.OutboxConfig;
import com.tradebridge.notification.domain.NotificationOutboxEntry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Binds the messaging-starter's outbox relay to the notification outbox
 * entity (spec §1.4).
 */
@Configuration
public class OutboxProvider {

    @Bean
    public OutboxConfig.OutboxEntityProvider notificationOutboxEntityProvider() {
        return () -> NotificationOutboxEntry.class;
    }
}