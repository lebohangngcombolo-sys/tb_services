package com.tradebridge.customer.config;

import com.tradebridge.customer.service.CustomerEventHooks;
import com.tradebridge.messaging.consumer.EventHandler;
import com.tradebridge.messaging.event.EventEnvelope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * Registers typed handlers for consumed event types, routing each to the
 * corresponding {@link CustomerEventHooks} method.
 */
@Configuration
public class EventHandlerContributor {

    @Bean
    public EventHandler<Map<String, Object>> userRegisteredHandler(CustomerEventHooks hooks) {
        return handler("identity.user.registered.v1",
                (envelope, payload) -> hooks.onUserRegistered(payload, envelope.getCorrelationId()));
    }

    @Bean
    public EventHandler<Map<String, Object>> userActivatedHandler(CustomerEventHooks hooks) {
        return handler("identity.user.activated.v1",
                (envelope, payload) -> hooks.onUserActivated(payload, envelope.getCorrelationId()));
    }

    @Bean
    public EventHandler<Map<String, Object>> userStatusChangedHandler(CustomerEventHooks hooks) {
        return handler("identity.user.status.changed.v1",
                (envelope, payload) -> hooks.onUserStatusChanged(payload, envelope.getCorrelationId()));
    }

    @Bean
    public EventHandler<Map<String, Object>> countryConfigurationPublishedHandler(CustomerEventHooks hooks) {
        return handler("country.configuration.published.v1",
                (envelope, payload) -> hooks.onCountryConfigurationPublished(payload, envelope.getCorrelationId()));
    }

    // ------------------------------------------------------------------ helpers

    @FunctionalInterface
    private interface Delegate {
        void handle(EventEnvelope envelope, Map<String, Object> payload);
    }

    @SuppressWarnings("unchecked")
    private static EventHandler<Map<String, Object>> handler(String eventType, Delegate delegate) {
        return new EventHandler<>() {
            @Override public String eventType() { return eventType; }
            @Override
            public Class<Map<String, Object>> payloadType() {
                return (Class<Map<String, Object>>) (Class<?>) Map.class;
            }
            @Override
            public void handle(EventEnvelope envelope, Map<String, Object> payload) {
                delegate.handle(envelope, payload);
            }
        };
    }
}