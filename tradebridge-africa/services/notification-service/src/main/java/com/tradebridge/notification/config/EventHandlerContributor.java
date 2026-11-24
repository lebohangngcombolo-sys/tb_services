package com.tradebridge.notification.config;

import com.tradebridge.messaging.consumer.EventHandler;
import com.tradebridge.messaging.event.EventEnvelope;
import com.tradebridge.notification.service.NotificationEventHooks;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.UUID;

/**
 * Registers one typed {@link EventHandler} per consumed event type,
 * routing each to the corresponding {@link NotificationEventHooks} method.
 *
 * <p>This class is the only place where the event-type → method mapping
 * lives; adding a new consumed event type requires one entry here (plus
 * the corresponding {@code EventTypeContributor} entry).
 */
@Configuration
public class EventHandlerContributor {

    @Bean
    public EventHandler<Map<String, Object>> identityUserRegisteredHandler(
            NotificationEventHooks hooks) {
        return handler("identity.user.registered.v1",
                (envelope, payload) -> hooks.onUserRegistered(payload, envelope.getCorrelationId()));
    }

    @Bean
    public EventHandler<Map<String, Object>> emailVerificationRequestedHandler(
            NotificationEventHooks hooks) {
        return handler("identity.user.email.verification.requested.v1",
                (envelope, payload) -> hooks.onEmailVerificationRequested(payload, envelope.getCorrelationId()));
    }

    @Bean
    public EventHandler<Map<String, Object>> passwordResetRequestedHandler(
            NotificationEventHooks hooks) {
        return handler("identity.user.password.reset.requested.v1",
                (envelope, payload) -> hooks.onPasswordResetRequested(payload, envelope.getCorrelationId()));
    }

    @Bean
    public EventHandler<Map<String, Object>> mfaOtpRequestedHandler(
            NotificationEventHooks hooks) {
        return handler("identity.user.mfa.otp.requested.v1",
                (envelope, payload) -> hooks.onMfaOtpRequested(payload, envelope.getCorrelationId()));
    }

    @Bean
    public EventHandler<Map<String, Object>> userActivatedHandler(NotificationEventHooks hooks) {
        return handler("identity.user.activated.v1",
                (envelope, payload) -> hooks.onUserActivated(payload, envelope.getCorrelationId()));
    }

    @Bean
    public EventHandler<Map<String, Object>> passwordChangedHandler(NotificationEventHooks hooks) {
        return handler("identity.user.password.changed.v1",
                (envelope, payload) -> hooks.onPasswordChanged(payload, envelope.getCorrelationId()));
    }

    @Bean
    public EventHandler<Map<String, Object>> organizationSubmittedHandler(
            NotificationEventHooks hooks) {
        return handler("customer.organization.submitted.v1",
                (envelope, payload) -> hooks.onOrganizationSubmitted(payload, envelope.getCorrelationId()));
    }

    @Bean
    public EventHandler<Map<String, Object>> kycReviewStartedHandler(
            NotificationEventHooks hooks) {
        return handler("customer.kyc.review.started.v1",
                (envelope, payload) -> hooks.onKycReviewStarted(payload, envelope.getCorrelationId()));
    }

    @Bean
    public EventHandler<Map<String, Object>> kycApprovedHandler(NotificationEventHooks hooks) {
        return handler("customer.kyc.approved.v1",
                (envelope, payload) -> hooks.onKycApproved(payload, envelope.getCorrelationId()));
    }

    @Bean
    public EventHandler<Map<String, Object>> kycRejectedHandler(NotificationEventHooks hooks) {
        return handler("customer.kyc.rejected.v1",
                (envelope, payload) -> hooks.onKycRejected(payload, envelope.getCorrelationId()));
    }

    @Bean
    public EventHandler<Map<String, Object>> countryConfigurationUpdatedHandler(
            NotificationEventHooks hooks) {
        return handler("country.configuration.updated.v1",
                (envelope, payload) -> hooks.onCountryConfigurationUpdated(payload, envelope.getCorrelationId()));
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
