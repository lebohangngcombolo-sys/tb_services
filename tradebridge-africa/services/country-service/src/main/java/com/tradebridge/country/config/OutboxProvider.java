package com.tradebridge.country.config;

import com.tradebridge.country.domain.CountryOutboxEntry;
import com.tradebridge.messaging.outbox.OutboxConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Binds the messaging-starter's outbox relay to the country-service's
 * outbox entity.
 */
@Configuration
public class OutboxProvider {

    @Bean
    public OutboxConfig.OutboxEntityProvider countryOutboxEntityProvider() {
        return () -> CountryOutboxEntry.class;
    }
}