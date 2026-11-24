package com.tradebridge.identity.config;

import com.tradebridge.identity.domain.IdentityInboxEntry;
import com.tradebridge.messaging.inbox.InboxConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Binds the messaging-starter's inbox guard to the identity-service's
 * inbox entity. Without this, consumer idempotency is bypassed
 * (spec §1.4 inbox requirement).
 */
@Configuration
public class InboxProvider {

    @Bean
    public InboxConfig.InboxEntityProvider identityInboxEntityProvider() {
        return () -> IdentityInboxEntry.class;
    }
}
