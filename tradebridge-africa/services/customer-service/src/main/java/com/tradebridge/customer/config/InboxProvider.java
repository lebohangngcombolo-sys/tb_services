package com.tradebridge.customer.config;

import com.tradebridge.customer.domain.CustomerInboxEntry;
import com.tradebridge.messaging.inbox.InboxConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class InboxProvider {

    @Bean
    public InboxConfig.InboxEntityProvider customerInboxEntityProvider() {
        return () -> CustomerInboxEntry.class;
    }
}