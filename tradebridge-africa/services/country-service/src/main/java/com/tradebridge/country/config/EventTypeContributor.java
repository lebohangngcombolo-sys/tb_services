package com.tradebridge.country.config;

import com.tradebridge.messaging.event.EventTypeRegistry;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

/**
 * Registers the event types the country service produces. Country has
 * no mandatory consumers, so no consumed types are registered here.
 */
@Configuration
public class EventTypeContributor {

    private final EventTypeRegistry registry;

    public EventTypeContributor(EventTypeRegistry registry) {
        this.registry = registry;
    }

    @PostConstruct
    public void register() {
        registry.register("country.created.v1");
        registry.register("country.updated.v1");
        registry.register("country.status.changed.v1");
        registry.register("country.currency.updated.v1");
        registry.register("country.language.updated.v1");
        registry.register("country.organization.requirement.updated.v1");
        registry.register("country.kyc.requirement.updated.v1");
        registry.register("country.configuration.published.v1");
    }
}