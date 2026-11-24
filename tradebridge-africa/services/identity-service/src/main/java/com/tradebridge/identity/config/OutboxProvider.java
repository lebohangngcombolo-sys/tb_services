package com.tradebridge.identity.config;

import com.tradebridge.identity.domain.IdentityOutboxEntry;
import com.tradebridge.messaging.outbox.OutboxConfig;
import com.tradebridge.messaging.outbox.OutboxEntry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Binds the messaging-starter's outbox relay to the identity-service's
 * outbox entity. Without this, the starter's outbox beans are not created
 * (spec §1.4 transactional outbox requirement).
 */
@Configuration
public class OutboxProvider {

    @Bean
    public OutboxConfig.OutboxEntityProvider identityOutboxEntityProvider() {
        return () -> IdentityOutboxEntry.class;
    }
}
