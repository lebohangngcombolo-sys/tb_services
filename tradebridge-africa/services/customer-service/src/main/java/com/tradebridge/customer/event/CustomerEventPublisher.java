package com.tradebridge.customer.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradebridge.common.id.UuidV7Generator;
import com.tradebridge.customer.event.payload.BeneficialOwnerDeclaredPayload;
import com.tradebridge.customer.event.payload.KycAdditionalInfoRequestedPayload;
import com.tradebridge.customer.event.payload.KycApprovedPayload;
import com.tradebridge.customer.event.payload.KycDecisionReversedPayload;
import com.tradebridge.customer.event.payload.KycRejectedPayload;
import com.tradebridge.customer.event.payload.KycResubmittedPayload;
import com.tradebridge.customer.event.payload.KycReviewStartedPayload;
import com.tradebridge.customer.event.payload.OrganizationCreatedPayload;
import com.tradebridge.customer.event.payload.OrganizationSubmittedPayload;
import com.tradebridge.customer.event.payload.OrganizationUpdatedPayload;
import com.tradebridge.customer.event.payload.OrganizationWithdrawnPayload;
import com.tradebridge.customer.event.payload.OwnershipDeclarationSubmittedPayload;
import com.tradebridge.customer.event.payload.ProfileCreatedPayload;
import com.tradebridge.customer.event.payload.RepresentativeAddedPayload;
import com.tradebridge.messaging.event.EventEnvelope;
import com.tradebridge.messaging.producer.EventPublisher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Publishes {@code customer.events} (spec §6.3) via the transactional
 * outbox (spec §1.4). Every method is {@code @Transactional} so the
 * outbox row is persisted atomically with the domain change.
 */
@Component
public class CustomerEventPublisher {

    public static final String TOPIC = "customer.events";
    public static final String PRODUCER = "customer-service";

    private final EventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    public CustomerEventPublisher(
            @Qualifier("transactionalEventPublisher") EventPublisher eventPublisher,
            ObjectMapper objectMapper) {
        this.eventPublisher = eventPublisher;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void publishProfileCreated(UUID customerId, UUID userId, String countryCode, UUID correlationId) {
        publish("customer.profile.created.v1", customerId,
                new ProfileCreatedPayload(customerId, userId, countryCode), correlationId);
    }

    @Transactional
    public void publishOrganizationCreated(UUID organizationId, String countryCode, UUID correlationId) {
        publish("customer.organization.created.v1", organizationId,
                new OrganizationCreatedPayload(organizationId, countryCode), correlationId);
    }

    @Transactional
    public void publishOrganizationUpdated(UUID organizationId, String changedFields, UUID correlationId) {
        publish("customer.organization.updated.v1", organizationId,
                new OrganizationUpdatedPayload(organizationId, changedFields), correlationId);
    }

    @Transactional
    public void publishOrganizationSubmitted(UUID organizationId, UUID kycCaseId,
                                              int configurationVersion, UUID correlationId) {
        publish("customer.organization.submitted.v1", organizationId,
                new OrganizationSubmittedPayload(organizationId, kycCaseId, configurationVersion),
                correlationId);
    }

    @Transactional
    public void publishOrganizationWithdrawn(UUID organizationId, String reason, UUID correlationId) {
        publish("customer.organization.withdrawn.v1", organizationId,
                new OrganizationWithdrawnPayload(organizationId, reason), correlationId);
    }

    @Transactional
    public void publishRepresentativeAdded(UUID organizationId, UUID representativeId,
                                            UUID correlationId) {
        publish("customer.representative.added.v1", organizationId,
                new RepresentativeAddedPayload(organizationId, representativeId), correlationId);
    }

    @Transactional
    public void publishBeneficialOwnerDeclared(UUID organizationId, UUID ownerId,
                                                UUID correlationId) {
        publish("customer.beneficial.owner.declared.v1", organizationId,
                new BeneficialOwnerDeclaredPayload(organizationId, ownerId), correlationId);
    }

    @Transactional
    public void publishOwnershipDeclarationSubmitted(UUID organizationId, UUID correlationId) {
        publish("customer.ownership.declaration.submitted.v1", organizationId,
                new OwnershipDeclarationSubmittedPayload(organizationId), correlationId);
    }

    @Transactional
    public void publishKycReviewStarted(UUID kycCaseId, UUID reviewerId, UUID correlationId) {
        publish("customer.kyc.review.started.v1", kycCaseId,
                new KycReviewStartedPayload(kycCaseId, reviewerId), correlationId);
    }

    @Transactional
    public void publishKycAdditionalInfoRequested(UUID kycCaseId, String reasonCode,
                                                    UUID correlationId) {
        publish("customer.kyc.additional.information.requested.v1", kycCaseId,
                new KycAdditionalInfoRequestedPayload(kycCaseId, reasonCode), correlationId);
    }

    @Transactional
    public void publishKycResubmitted(UUID kycCaseId, UUID correlationId) {
        publish("customer.kyc.resubmitted.v1", kycCaseId,
                new KycResubmittedPayload(kycCaseId), correlationId);
    }

    @Transactional
    public void publishKycApproved(UUID organizationId, UUID kycCaseId, UUID decisionId,
                                     UUID correlationId) {
        publish("customer.kyc.approved.v1", kycCaseId,
                new KycApprovedPayload(organizationId, kycCaseId, decisionId), correlationId);
    }

    @Transactional
    public void publishKycRejected(UUID organizationId, UUID kycCaseId, UUID decisionId,
                                     UUID correlationId) {
        publish("customer.kyc.rejected.v1", kycCaseId,
                new KycRejectedPayload(organizationId, kycCaseId, decisionId), correlationId);
    }

    @Transactional
    public void publishKycDecisionReversed(UUID kycCaseId, UUID previousDecisionId,
                                            UUID newDecisionId, UUID correlationId) {
        publish("customer.kyc.decision.reversed.v1", kycCaseId,
                new KycDecisionReversedPayload(kycCaseId, previousDecisionId, newDecisionId),
                correlationId);
    }

    // ------------------------------------------------------------------ internal

    private void publish(String eventType, UUID aggregateId, Object payload, UUID correlationId) {
        int version = parseVersion(eventType);
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