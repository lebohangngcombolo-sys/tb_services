package com.tradebridge.notification.service;

import com.tradebridge.common.enums.Channel;
import com.tradebridge.common.enums.Priority;
import com.tradebridge.common.util.StringUtils;
import com.tradebridge.notification.config.NotificationProperties;
import com.tradebridge.notification.repository.NotificationTemplateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Business logic for each notification-triggering event (spec §5.3
 * "Consumed events"). Called by {@code EventHandlerContributor} when a
 * matching Kafka record arrives.
 *
 * <p>Design intent:
 * <ul>
 *   <li>Never logs PII, tokens, or OTP codes.</li>
 *   <li>Consults the identity-service internal endpoint for the raw OTP
 *       only when required (via {@link ChallengeDeliveryClient}).</li>
 *   <li>Uses the submission service for the actual persistence + delivery.</li>
 * </ul>
 */
@Service
public class NotificationEventHooks {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventHooks.class);

    private final NotificationSubmissionService submissionService;
    private final ChallengeDeliveryClient challengeDeliveryClient;
    private final NotificationTemplateRepository templateRepository;
    private final NotificationProperties properties;

    public NotificationEventHooks(NotificationSubmissionService submissionService,
                                    ChallengeDeliveryClient challengeDeliveryClient,
                                    NotificationTemplateRepository templateRepository,
                                    NotificationProperties properties) {
        this.submissionService = submissionService;
        this.challengeDeliveryClient = challengeDeliveryClient;
        this.templateRepository = templateRepository;
        this.properties = properties;
    }

    // ============================================================ IDENTITY

    @Transactional
    public void onUserRegistered(Map<String, Object> payload, UUID correlationId) {
        // identity.user.registered.v1 → send email verification
        // Challenge + raw OTP arrive via a separate *.requested event, not here.
        String userId = str(payload, "userId");
        String email = str(payload, "email");
        String countryCode = str(payload, "countryCode");
        log.info("User registered — logging only (verification sent via .requested.v1): userId={} country={}",
                userId, countryCode);
        // We do not send anything here — the verification email is triggered
        // by the separate identity.user.email.verification.requested.v1 event
        // (see onEmailVerificationRequested). This avoids duplicate sends.
    }

    @Transactional
    public void onEmailVerificationRequested(Map<String, Object> payload, UUID correlationId) {
        String userId = str(payload, "userId");
        String challengeId = str(payload, "challengeId");
        if (userId == null || challengeId == null) {
            log.warn("email.verification.requested missing required fields");
            return;
        }
        deliverOtp("identity.verification", UUID.fromString(userId),
                UUID.fromString(challengeId), correlationId);
    }

    @Transactional
    public void onPasswordResetRequested(Map<String, Object> payload, UUID correlationId) {
        String userId = str(payload, "userId");
        String challengeId = str(payload, "challengeId");
        if (userId == null || challengeId == null) {
            log.warn("password.reset.requested missing required fields");
            return;
        }
        deliverOtp("identity.password.reset", UUID.fromString(userId),
                UUID.fromString(challengeId), correlationId);
    }

    @Transactional
    public void onMfaOtpRequested(Map<String, Object> payload, UUID correlationId) {
        String userId = str(payload, "userId");
        String challengeId = str(payload, "challengeId");
        if (userId == null || challengeId == null) {
            log.warn("mfa.otp.requested missing required fields");
            return;
        }
        deliverOtp("identity.mfa.otp", UUID.fromString(userId),
                UUID.fromString(challengeId), correlationId);
    }

    @Transactional
    public void onUserActivated(Map<String, Object> payload, UUID correlationId) {
        String userId = str(payload, "userId");
        if (userId == null) return;
        String email = str(payload, "email"); // not present in spec — best-effort; consumer may not send
        // Account activation email — informational only
        // We cannot send without an email destination; if absent we simply log.
        if (email == null || email.isBlank()) {
            log.debug("Activation event for user {} has no email in payload — skipping email",
                    userId);
            return;
        }
        Map<String, Object> vars = Map.of(
                "userId", userId,
                "message", "Your TradeBridge Africa account is now active.");
        submitNonOtp(Channel.EMAIL, "identity.account.activated", "en",
                email, UUID.fromString(userId), vars, correlationId);
    }

    @Transactional
    public void onPasswordChanged(Map<String, Object> payload, UUID correlationId) {
        // Security alert — informational, but only if the payload carries an email.
        String userId = str(payload, "userId");
        String email = str(payload, "email");
        if (userId == null || email == null || email.isBlank()) {
            log.debug("password.changed for user {} has no email — security alert skipped (logged)",
                    userId);
            return;
        }
        Map<String, Object> vars = Map.of(
                "userId", userId,
                "message", "Your TradeBridge Africa password was changed. If this was not you, contact support immediately.");
        submitNonOtp(Channel.EMAIL, "identity.password.changed", "en",
                email, UUID.fromString(userId), vars, correlationId);
    }

    // ============================================================ CUSTOMER

    @Transactional
    public void onOrganizationSubmitted(Map<String, Object> payload, UUID correlationId) {
        String organizationId = str(payload, "organizationId");
        String kycCaseId = str(payload, "kycCaseId");
        String configurationVersion = str(payload, "configurationVersion");
        log.info("Organization submitted: org={} case={} configVersion={}",
                organizationId, kycCaseId, configurationVersion);
        // Confirmation email is sent to the customer if the payload includes
        // an email. Customer-service does not include email in the event per
        // spec §6.3 (only IDs), so we log only. When customer-service is
        // extended to include a contact email in the event, this method
        // sends the confirmation.
    }

    @Transactional
    public void onKycReviewStarted(Map<String, Object> payload, UUID correlationId) {
        String kycCaseId = str(payload, "kycCaseId");
        String reviewerId = str(payload, "reviewerId");
        log.info("KYC review started: case={} reviewer={}", kycCaseId, reviewerId);
        // Notify customer that review has begun — requires email in payload
        // which is currently absent per spec §6.3. Log only.
    }

    @Transactional
    public void onKycApproved(Map<String, Object> payload, UUID correlationId) {
        String organizationId = str(payload, "organizationId");
        String kycCaseId = str(payload, "kycCaseId");
        String decisionId = str(payload, "decisionId");
        String email = str(payload, "email");
        log.info("KYC approved: org={} case={} decision={}", organizationId, kycCaseId, decisionId);
        if (email != null && !email.isBlank()) {
            Map<String, Object> vars = new HashMap<>();
            vars.put("kycCaseId", kycCaseId);
            vars.put("decisionId", decisionId);
            vars.put("message", "Your KYC application has been approved.");
            submitNonOtp(Channel.EMAIL, "customer.kyc.approved", "en",
                    email, null, vars, correlationId);
        }
    }

    @Transactional
    public void onKycRejected(Map<String, Object> payload, UUID correlationId) {
        String organizationId = str(payload, "organizationId");
        String kycCaseId = str(payload, "kycCaseId");
        String decisionId = str(payload, "decisionId");
        String email = str(payload, "email");
        String reason = str(payload, "reasonCode");
        log.info("KYC rejected: org={} case={} decision={}", organizationId, kycCaseId, decisionId);
        if (email != null && !email.isBlank()) {
            Map<String, Object> vars = new HashMap<>();
            vars.put("kycCaseId", kycCaseId);
            vars.put("decisionId", decisionId);
            vars.put("message", "Your KYC application was not approved."
                    + (reason != null ? " Reason code: " + reason : ""));
            submitNonOtp(Channel.EMAIL, "customer.kyc.rejected", "en",
                    email, null, vars, correlationId);
        }
    }

    // ============================================================ COUNTRY

    @Transactional
    public void onCountryConfigurationUpdated(Map<String, Object> payload, UUID correlationId) {
        String countryCode = str(payload, "countryCode");
        Object version = payload.get("version");
        // Country Service is authoritative; notification-service simply logs.
        // Template or config-cache refresh can be triggered here if needed
        // in a future iteration.
        log.info("Country configuration updated: {} v{}", countryCode, version);
    }

    // ============================================================ helpers

    /**
     * Fetches the raw code and destination from identity-service using its
     * single-use handoff, then submits through the standard pipeline.
     *
     * <p>OTP body is sent to the provider but never persisted —
     * {@link NotificationSubmissionService} skips body storage for
     * OTP-sensitive templates.
     */
    private void deliverOtp(String templateCode, UUID userId, UUID challengeId,
                            UUID correlationId) {
        var delivery = challengeDeliveryClient.fetchChallenge(challengeId);
        if (delivery.isEmpty()) {
            log.warn("OTP challenge {} could not be fetched (expired or consumed) — "
                    + "skipping notification for template {}",
                    challengeId, templateCode);
            return;
        }
        String rawCode = delivery.get().code();
        String destination = delivery.get().destination();

        Channel channel = Channel.EMAIL;
        Map<String, Object> vars = new HashMap<>();
        vars.put("otpCode", rawCode);
        vars.put("expiryMinutes", "10");
        submitNonOtp(channel, templateCode, "en", destination, userId, vars, correlationId);
    }

    private void submitNonOtp(Channel channel, String templateCode, String languageCode,
                               String destination, UUID userId, Map<String, Object> vars,
                               UUID correlationId) {
        // Only send if a template is configured for this channel+language
        var candidates = templateRepository.findActiveByCodeAndChannelAndLanguage(
                templateCode, channel, languageCode.toLowerCase());
        if (candidates.isEmpty()) {
            log.debug("No template for {} — notification not sent", templateCode);
            return;
        }
        switch (channel) {
            case EMAIL -> submissionService.submitEmail(
                    new com.tradebridge.notification.api.dto.request.SubmitEmailRequest(
                            templateCode, languageCode, destination, vars,
                            Priority.NORMAL, userId, correlationId.toString(), null, null));
            case SMS -> submissionService.submitSms(
                    new com.tradebridge.notification.api.dto.request.SubmitSmsRequest(
                            templateCode, languageCode, destination, vars,
                            Priority.NORMAL, userId, correlationId.toString(), null, null));
            case IN_APP -> submissionService.submitInApp(
                    new com.tradebridge.notification.api.dto.request.SubmitInAppRequest(
                            templateCode, languageCode, userId, vars,
                            Priority.NORMAL, correlationId.toString(), null));
        }
    }

    private static String str(Map<String, Object> payload, String key) {
        Object v = payload == null ? null : payload.get(key);
        return v == null ? null : v.toString();
    }
}