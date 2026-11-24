package com.tradebridge.identity.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradebridge.messaging.event.EventEnvelope;
import com.tradebridge.messaging.producer.EventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

class IdentityEventPublisherTest {

    @Mock private EventPublisher eventPublisher;

    private IdentityEventPublisher publisher;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        publisher = new IdentityEventPublisher(eventPublisher, new ObjectMapper());
    }

    @Test
    void publishesUserRegisteredToIdentityTopic() {
        UUID userId = UUID.randomUUID();
        UUID correlationId = UUID.randomUUID();

        publisher.publishUserRegistered(userId, "x@y.com", "ZA", correlationId);

        ArgumentCaptor<EventEnvelope> captor = ArgumentCaptor.forClass(EventEnvelope.class);
        verify(eventPublisher).publish(eq("identity.events"), captor.capture());

        EventEnvelope envelope = captor.getValue();
        assertThat(envelope.getEventType()).isEqualTo("identity.user.registered.v1");
        assertThat(envelope.getEventVersion()).isEqualTo(1);
        assertThat(envelope.getAggregateId()).isEqualTo(userId);
        assertThat(envelope.getProducer()).isEqualTo("identity-service");
        assertThat(envelope.getCorrelationId()).isEqualTo(correlationId);
        assertThat(envelope.getPayload()).isInstanceOf(Map.class);

        @SuppressWarnings("unchecked")
        Map<String, Object> payload = (Map<String, Object>) envelope.getPayload();
        assertThat(payload).containsEntry("email", "x@y.com").containsEntry("countryCode", "ZA");
    }

    @Test
    void publishesMfaEnabledWithFactorType() {
        UUID userId = UUID.randomUUID();
        publisher.publishMfaEnabled(userId, "TOTP", UUID.randomUUID());

        ArgumentCaptor<EventEnvelope> captor = ArgumentCaptor.forClass(EventEnvelope.class);
        verify(eventPublisher).publish(any(), captor.capture());
        EventEnvelope envelope = captor.getValue();

        assertThat(envelope.getEventType()).isEqualTo("identity.user.mfa.enabled.v1");
        @SuppressWarnings("unchecked")
        Map<String, Object> payload = (Map<String, Object>) envelope.getPayload();
        assertThat(payload).containsEntry("factorType", "TOTP");
    }

    @Test
    void publishesRoleChangedWithAllCodes() {
        UUID userId = UUID.randomUUID();
        publisher.publishRoleChanged(userId, List.of("ADMIN", "KYC_ANALYST"), UUID.randomUUID());

        ArgumentCaptor<EventEnvelope> captor = ArgumentCaptor.forClass(EventEnvelope.class);
        verify(eventPublisher).publish(any(), captor.capture());
        EventEnvelope envelope = captor.getValue();
        assertThat(envelope.getEventType()).isEqualTo("identity.user.role.changed.v1");
        @SuppressWarnings("unchecked")
        Map<String, Object> payload = (Map<String, Object>) envelope.getPayload();
        assertThat((List<String>) payload.get("roleCodes")).containsExactly("ADMIN", "KYC_ANALYST");
    }
}
