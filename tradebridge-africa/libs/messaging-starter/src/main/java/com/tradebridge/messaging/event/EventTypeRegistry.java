package com.tradebridge.messaging.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Whitelist of event types the service is willing to publish or consume.
 *
 * <p>Publishing or accepting an unknown type is treated as a configuration
 * error — it protects against typos in event names and against accidental
 * backward-incompatible renames (§7.3: "Event schema changes must remain
 * backward-compatible where possible").
 *
 * <p>Services register their event types via the starter's auto-configuration
 * (e.g. {@code EventTypeRegistry.add("identity.user.registered.v1")}) or by
 * declaring an {@code EventTypeContributor} bean.
 */
public class EventTypeRegistry {

    private static final Logger log = LoggerFactory.getLogger(EventTypeRegistry.class);

    private final Set<String> knownTypes = ConcurrentHashMap.newKeySet();

    public void register(String eventType) {
        if (eventType == null || eventType.isBlank()) {
            throw new IllegalArgumentException("eventType must not be blank");
        }
        if (!eventType.matches("^[a-z][a-z0-9]*(\\.[a-z0-9]+)+\\.v\\d+$")) {
            throw new IllegalArgumentException(
                    "Invalid eventType (expected 'domain.name.v<n>'): " + eventType);
        }
        if (knownTypes.add(eventType)) {
            log.debug("Registered event type {}", eventType);
        }
    }

    public void registerAll(Set<String> types) {
        if (types != null) types.forEach(this::register);
    }

    public boolean isKnown(String eventType) {
        return eventType != null && knownTypes.contains(eventType);
    }

    public void requireKnown(String eventType) {
        if (!isKnown(eventType)) {
            throw new IllegalArgumentException("Unregistered event type: " + eventType);
        }
    }

    public Set<String> knownTypes() {
        return Collections.unmodifiableSet(knownTypes);
    }

    public Optional<String> matchDomain(String domain) {
        return knownTypes.stream().filter(t -> t.startsWith(domain + ".")).findFirst();
    }
}
