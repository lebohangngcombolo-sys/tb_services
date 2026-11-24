package com.tradebridge.customer.config;

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
        // ----- Published (spec §6.3)
        registry.register("customer.profile.created.v1");
        registry.register("customer.organization.created.v1");
        registry.register("customer.organization.updated.v1");
        registry.register("customer.organization.submitted.v1");
        registry.register("customer.organization.withdrawn.v1");
        registry.register("customer.representative.added.v1");
        registry.register("customer.beneficial.owner.declared.v1");
        registry.register("customer.ownership.declaration.submitted.v1");
        registry.register("customer.kyc.review.started.v1");
        registry.register("customer.kyc.additional.information.requested.v1");
        registry.register("customer.kyc.resubmitted.v1");
        registry.register("customer.kyc.approved.v1");
        registry.register("customer.kyc.rejected.v1");
        registry.register("customer.kyc.decision.reversed.v1");

        // ----- Consumed (spec §6.3)
        registry.register("identity.user.registered.v1");
        registry.register("identity.user.activated.v1");
        registry.register("identity.user.status.changed.v1");
        registry.register("country.configuration.published.v1");
    }
}