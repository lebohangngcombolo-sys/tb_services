package com.tradebridge.country.cache;

import com.tradebridge.country.domain.CountryOutboxEntry;
import com.tradebridge.messaging.outbox.OutboxStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Evicts cached country data when its configuration or requirements change.
 *
 * <p>The country-service is its own producer — {@code country.events} are
 * persisted to the outbox and then published to Kafka, but the service does
 * not consume its own topic (that would be self-consuming). Instead, this
 * scheduled listener scans the outbox for events of interest and evicts
 * the affected cache entries <b>after</b> the outbox relay has published them.
 *
 * <p>Effect: caches become coherent within one polling interval of a
 * configuration change. Combined with the 5-minute TTL, the worst-case
 * staleness window is bounded and small.
 */
@Component
public class CacheEvictionListener {

    private static final Logger log = LoggerFactory.getLogger(CacheEvictionListener.class);

    private static final List<String> EVICTION_TRIGGER_TYPES = List.of(
            "country.configuration.published.v1",
            "country.kyc.requirement.updated.v1",
            "country.organization.requirement.updated.v1"
    );

    @PersistenceContext
    private EntityManager entityManager;

    private final CountryConfigCache cache;

    /** Last outbox row scanned — used as a monotonic cursor for new events. */
    private volatile java.util.UUID lastScannedOutboxId = null;

    public CacheEvictionListener(CountryConfigCache cache) {
        this.cache = cache;
    }

    @Scheduled(fixedDelayString = "${tradebridge.country.cache.eviction-poll-ms:5000}")
    @Transactional(readOnly = true)
    public void pollOutbox() {
        // Scan PUBLISHED rows that have not yet been processed.
        String query = lastScannedOutboxId == null
                ? "SELECT o FROM CountryOutboxEntry o WHERE o.status = :s "
                    + "AND o.eventType IN :types ORDER BY o.createdAt ASC"
                : "SELECT o FROM CountryOutboxEntry o WHERE o.status = :s "
                    + "AND o.eventType IN :types AND o.id > :lastId ORDER BY o.createdAt ASC";

        var typedQuery = entityManager
                .createQuery(query, CountryOutboxEntry.class)
                .setParameter("s", OutboxStatus.PUBLISHED)
                .setParameter("types", EVICTION_TRIGGER_TYPES)
                .setMaxResults(100);
        if (lastScannedOutboxId != null) {
            typedQuery.setParameter("lastId", lastScannedOutboxId);
        }

        List<CountryOutboxEntry> events = typedQuery.getResultList();
        if (events.isEmpty()) {
            return;
        }

        for (CountryOutboxEntry event : events) {
            evictForEvent(event);
            lastScannedOutboxId = event.getId();
        }
        log.debug("Processed {} country event(s) for cache eviction", events.size());
    }

    private void evictForEvent(CountryOutboxEntry event) {
        try {
            // Payload is stored as JSON string; extract "countryCode" cheaply.
            String payload = event.getPayload();
            int idx = payload.indexOf("\"countryCode\"");
            if (idx < 0) {
                return;
            }
            int colon = payload.indexOf(':', idx);
            int quoteStart = payload.indexOf('"', colon + 1);
            int quoteEnd = payload.indexOf('"', quoteStart + 1);
            if (quoteStart < 0 || quoteEnd < 0) {
                return;
            }
            String countryCode = payload.substring(quoteStart + 1, quoteEnd);
            cache.evictCountry(countryCode);
            log.info("Cache evicted for country {} due to {}", countryCode, event.getEventType());
        } catch (Exception e) {
            log.warn("Failed to evict cache for event {}: {}",
                    event.getEventType(), e.getMessage());
        }
    }
}