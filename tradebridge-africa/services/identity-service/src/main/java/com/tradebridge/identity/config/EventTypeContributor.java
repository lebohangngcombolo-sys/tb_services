package com.tradebridge.identity.config;

import com.tradebridge.messaging.event.EventTypeRegistry;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

/**
 * Registers all event types the identity service produces or consumes
 * with the shared {@link EventTypeRegistry}. Unknown event types are
 * rejected by the consumer base (spec §7.3 "Event ID must be present;
 * event type and version must be supported").
 */
@Configuration
public class EventTypeContributor {

    private final EventTypeRegistry registry;

    public EventTypeContributor(EventTypeRegistry registry) {
        this.registry = registry;
    }

    @PostConstruct
    public void register() {
        // Published
        registry.register("identity.user.registered.v1");
        registry.register("identity.user.email.verification.requested.v1");
        registry.register("identity.user.activated.v1");
        registry.register("identity.user.password.reset.requested.v1");
        registry.register("identity.user.password.changed.v1");
        registry.register("identity.user.mfa.otp.requested.v1");
        registry.register("identity.user.mfa.enabled.v1");
        registry.register("identity.user.mfa.disabled.v1");
        registry.register("identity.user.role.changed.v1");
        registry.register("identity.user.status.changed.v1");
        registry.register("identity.user.session.revoked.v1");

        // Consumed
        registry.register("notification.delivery.failed.v1");
        registry.register("country.configuration.updated.v1");
    }
}
