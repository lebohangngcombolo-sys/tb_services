package com.tradebridge.messaging.inbox;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the {@link InboxGuard} only when the service declares an
 * {@link InboxEntityProvider} bean.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnBean(InboxConfig.InboxEntityProvider.class)
public class InboxConfig {

    @Bean
    @ConditionalOnMissingBean
    public InboxGuard inboxGuard(InboxEntityProvider provider) {
        return new InboxGuard(provider.inboxEntityClass());
    }

    public interface InboxEntityProvider {
        Class<? extends InboxEntry> inboxEntityClass();
    }
}
