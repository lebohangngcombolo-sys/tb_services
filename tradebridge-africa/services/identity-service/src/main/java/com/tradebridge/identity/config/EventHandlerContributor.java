package com.tradebridge.identity.config;

import com.tradebridge.common.id.UuidV7Generator;
import com.tradebridge.identity.domain.IdentityAuditRecord;
import com.tradebridge.identity.domain.enums.IdentityAction;
import com.tradebridge.identity.repository.IdentityAuditRecordRepository;
import com.tradebridge.messaging.consumer.EventHandler;
import com.tradebridge.messaging.event.EventEnvelope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Registers typed handlers for consumed events. The base consumer
 * dispatches to these via {@code ConsumerRegistry}.
 */
@Configuration
public class EventHandlerContributor {

    private static final Logger log = LoggerFactory.getLogger(EventHandlerContributor.class);

    @Bean
    public EventHandler<Map<String, Object>> notificationDeliveryFailedHandler(
            IdentityAuditRecordRepository auditRepository) {
        return new EventHandler<>() {
            @Override
            public String eventType() {
                return "notification.delivery.failed.v1";
            }

            @Override
            public Class<Map<String, Object>> payloadType() {
                return castMapClass();
            }

            @Override
            public void handle(EventEnvelope envelope, Map<String, Object> payload) {
                String notificationId = String.valueOf(payload.getOrDefault("notificationId", ""));
                String reason = String.valueOf(payload.getOrDefault("failureReason", ""));
                String userId = String.valueOf(payload.getOrDefault("recipientUserId", ""));
                UUID userIdUuid = parseUuid(userId);
                auditRepository.save(new IdentityAuditRecord(
                        userIdUuid, null, IdentityAction.MFA_CHALLENGE_FAILED, null,
                        Map.of("notificationId", notificationId,
                               "reason", reason,
                               "source", "notification.delivery.failed")));
                log.info("Recorded notification delivery failure for userId={} notificationId={}",
                        userIdUuid, notificationId);
            }
        };
    }

    @Bean
    public EventHandler<Map<String, Object>> countryConfigurationUpdatedHandler() {
        return new EventHandler<>() {
            @Override
            public String eventType() {
                return "country.configuration.updated.v1";
            }

            @Override
            public Class<Map<String, Object>> payloadType() {
                return castMapClass();
            }

            @Override
            public void handle(EventEnvelope envelope, Map<String, Object> payload) {
                String countryCode = String.valueOf(payload.getOrDefault("countryCode", ""));
                Object version = payload.getOrDefault("version", "");
                log.info("Country configuration updated: {} v{} — will validate on next registration",
                        countryCode, version);
            }
        };
    }

    @SuppressWarnings("unchecked")
    private static Class<Map<String, Object>> castMapClass() {
        return (Class<Map<String, Object>>) (Class<?>) Map.class;
    }

    private static UUID parseUuid(String value) {
        if (value == null || value.isBlank() || "null".equals(value)) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
