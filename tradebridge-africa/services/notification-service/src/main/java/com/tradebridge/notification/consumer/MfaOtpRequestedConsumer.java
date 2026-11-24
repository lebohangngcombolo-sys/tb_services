package com.tradebridge.notification.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradebridge.messaging.config.MessagingProperties;
import com.tradebridge.messaging.consumer.BaseConsumer;
import com.tradebridge.messaging.consumer.ConsumerRegistry;
import com.tradebridge.messaging.dlq.DeadLetterRouter;
import com.tradebridge.messaging.event.EventEnvelope;
import com.tradebridge.messaging.event.EventTypeRegistry;
import com.tradebridge.messaging.inbox.InboxGuard;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumes {@code identity.user.mfa.otp.requested.v1}. The payload carries
 * only the challenge ID — the raw OTP is fetched via
 * {@link com.tradebridge.notification.service.ChallengeDeliveryClient}
 * (spec §3.3 "OTP delivery events must not contain raw OTP values").
 */
@Component
public class MfaOtpRequestedConsumer extends BaseConsumer {

    public static final String CONSUMER_NAME = "notification.identity.mfa.otp.requested";

    public MfaOtpRequestedConsumer(ConsumerRegistry registry,
                                    InboxGuard inboxGuard,
                                    EventTypeRegistry typeRegistry,
                                    DeadLetterRouter dlqRouter,
                                    ObjectMapper objectMapper,
                                    MessagingProperties messagingProperties) {
        super(registry, inboxGuard, typeRegistry, dlqRouter, objectMapper,
                messagingProperties.getConsumer(), CONSUMER_NAME);
    }

    @KafkaListener(
            topics = "identity.events",
            groupId = "notification-service",
            containerFactory = "envelopeKafkaListenerContainerFactory")
    public void onMessage(ConsumerRecord<String, EventEnvelope> record) {
        handleRecord(record);
    }
}