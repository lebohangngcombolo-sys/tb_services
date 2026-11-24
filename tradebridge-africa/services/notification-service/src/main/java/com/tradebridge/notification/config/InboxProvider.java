package com.tradebridge.notification.config;

import com.tradebridge.messaging.inbox.InboxConfig;
import com.tradebridge.notification.domain.NotificationInboxEntry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Binds the messaging-starter's inbox guard to the notification inbox
 * entity (spec §1.4).
 */
@Configuration
public class InboxProvider {

    @Bean
    public InboxConfig.InboxEntityProvider notificationInboxEntityProvider() {
        return () -> NotificationInboxEntry.class;
    }
}