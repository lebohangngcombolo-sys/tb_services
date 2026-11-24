package com.tradebridge.country.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradebridge.common.id.UuidV7Generator;
import com.tradebridge.country.event.payload.ConfigurationPublishedPayload;
import com.tradebridge.country.event.payload.CountryCreatedPayload;
import com.tradebridge.country.event.payload.CountryStatusChangedPayload;
import com.tradebridge.country.event.payload.CountryUpdatedPayload;
import com.tradebridge.country.event.payload.CurrencyUpdatedPayload;
import com.tradebridge.country.event.payload.KycRequirementUpdatedPayload;
import com.tradebridge.country.event.payload.LanguageUpdatedPayload;
import com.tradebridge.country.event.payload.OrganizationRequirementUpdatedPayload;
import com.tradebridge.messaging.event.EventEnvelope;
import com.tradebridge.messaging.producer.EventPublisher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Publishes {@code country.events} (spec §4.3) via the transactional outbox.
 *
 * <p>Every method is {@code @Transactional} so the outbox row is persisted
 * in the same transaction as the domain change (spec §1.4).
 */
@Component
public class CountryEventPublisher {

    public static final String TOPIC = "country.events";
    public static final String PRODUCER = "country-service";

    private final EventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    public CountryEventPublisher(
            @Qualifier("transactionalEventPublisher") EventPublisher eventPublisher,
            ObjectMapper objectMapper) {
        this.eventPublisher = eventPublisher;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void publishCountryCreated(String countryCode, String name, UUID correlationId) {
        publish("country.created.v1", countryCode,
                new CountryCreatedPayload(countryCode, name), correlationId);
    }

    @Transactional
    public void publishCountryUpdated(String countryCode, String changedFields, UUID correlationId) {
        publish("country.updated.v1", countryCode,
                new CountryUpdatedPayload(countryCode, changedFields), correlationId);
    }

    @Transactional
    public void publishCountryStatusChanged(String countryCode, boolean enabled, UUID correlationId) {
        publish("country.status.changed.v1", countryCode,
                new CountryStatusChangedPayload(countryCode, enabled), correlationId);
    }

    @Transactional
    public void publishCurrencyUpdated(String countryCode, String currencyCode, UUID correlationId) {
        publish("country.currency.updated.v1", countryCode,
                new CurrencyUpdatedPayload(countryCode, currencyCode), correlationId);
    }

    @Transactional
    public void publishLanguageUpdated(String countryCode, String languageCode, UUID correlationId) {
        publish("country.language.updated.v1", countryCode,
                new LanguageUpdatedPayload(countryCode, languageCode), correlationId);
    }

    @Transactional
    public void publishOrganizationRequirementUpdated(String countryCode, String requirementCode,
                                                       UUID correlationId) {
        publish("country.organization.requirement.updated.v1", countryCode,
                new OrganizationRequirementUpdatedPayload(countryCode, requirementCode),
                correlationId);
    }

    @Transactional
    public void publishKycRequirementUpdated(String countryCode, String requirementCode,
                                              UUID correlationId) {
        publish("country.kyc.requirement.updated.v1", countryCode,
                new KycRequirementUpdatedPayload(countryCode, requirementCode), correlationId);
    }

    @Transactional
    public void publishConfigurationPublished(String countryCode, int version, UUID correlationId) {
        publish("country.configuration.published.v1", countryCode,
                new ConfigurationPublishedPayload(countryCode, version), correlationId);
    }

    // ------------------------------------------------------------------ internal

    private void publish(String eventType, String aggregateKey, Object payload, UUID correlationId) {
        int version = parseVersion(eventType);
        // aggregateId for country events = a stable UUID derived from the ISO2 code
        // (aggregate key on Kafka is the string form of aggregateId).
        UUID aggregateId = UUID.nameUUIDFromBytes(("country:" + aggregateKey).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        Map<String, Object> payloadMap = objectMapper.convertValue(payload, Map.class);

        EventEnvelope envelope = new EventEnvelope(
                UuidV7Generator.next(),
                eventType,
                version,
                aggregateId,
                Instant.now(),
                PRODUCER,
                correlationId,
                null,
                payloadMap);

        eventPublisher.publish(TOPIC, envelope);
    }

    private static int parseVersion(String eventType) {
        int dotV = eventType.lastIndexOf(".v");
        if (dotV < 0) throw new IllegalArgumentException("Event type must end with '.v<n>': " + eventType);
        return Integer.parseInt(eventType.substring(dotV + 2));
    }
}