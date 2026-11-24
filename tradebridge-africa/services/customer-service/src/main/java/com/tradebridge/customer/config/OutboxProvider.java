package com.tradebridge.customer.config;

import com.tradebridge.customer.domain.CustomerOutboxEntry;
import com.tradebridge.messaging.outbox.OutboxConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OutboxProvider {

    @Bean
    public OutboxConfig.OutboxEntityProvider customerOutboxEntityProvider() {
        return () -> CustomerOutboxEntry.class;
    }
}
