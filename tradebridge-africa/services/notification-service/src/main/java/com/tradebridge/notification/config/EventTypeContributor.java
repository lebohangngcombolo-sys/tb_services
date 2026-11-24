package com.tradebridge.notification.config;

import com.tradebridge.messaging.event.EventTypeRegistry;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

/**
 * Registers every event type this service produces or consumes with the
 * shared {@link EventTypeRegistry}. Unknown types are rejected by
 * {@code BaseConsumer} and routed to DLQ (spec §7.3).
 */
@Configuration
public class EventTypeContributor {

    private final EventTypeRegistry registry;

    public EventTypeContributor(EventTypeRegistry registry) {
        this.registry = registry;
    }

    @PostConstruct
    public void register() {
        // ----- Published
        registry.register("notification.delivery.requested.v1");
        registry.register("notification.delivery.succeeded.v1");
        registry.register("notification.delivery.failed.v1");
        registry.register("notification.inapp.created.v1");
        registry.register("notification.inapp.read.v1");
        registry.register("notification.delivery.exhausted.v1");

        // ----- Consumed (identity.events)
        registry.register("identity.user.registered.v1");
        registry.register("identity.user.email.verification.requested.v1");
        registry.register("identity.user.password.reset.requested.v1");
        registry.register("identity.user.mfa.otp.requested.v1");
        registry.register("identity.user.activated.v1");
        registry.register("identity.user.password.changed.v1");

        // ----- Consumed (customer.events)
        registry.register("customer.organization.submitted.v1");
        registry.register("customer.kyc.review.started.v1");
        registry.register("customer.kyc.approved.v1");
        registry.register("customer.kyc.rejected.v1");

        // ----- Consumed (country.events)
        registry.register("country.configuration.updated.v1");
    }
}
