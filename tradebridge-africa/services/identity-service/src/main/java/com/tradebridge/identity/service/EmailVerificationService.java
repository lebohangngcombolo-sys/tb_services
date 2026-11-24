package com.tradebridge.identity.service;

import com.tradebridge.common.context.CorrelationContext;
import com.tradebridge.common.exception.BusinessException;
import com.tradebridge.common.exception.ErrorCode;
import com.tradebridge.common.exception.ResourceNotFoundException;
import com.tradebridge.common.exception.ValidationException;
import com.tradebridge.identity.api.dto.request.VerifyEmailRequest;
import com.tradebridge.identity.config.IdentityProperties;
import com.tradebridge.identity.domain.User;
import com.tradebridge.identity.domain.VerificationChallenge;
import com.tradebridge.identity.domain.enums.ChallengeType;
import com.tradebridge.identity.domain.enums.IdentityAction;
import com.tradebridge.identity.domain.enums.UserStatus;
import com.tradebridge.identity.event.IdentityEventPublisher;
import com.tradebridge.identity.repository.IdentityAuditRecordRepository;
import com.tradebridge.identity.repository.UserRepository;
import com.tradebridge.identity.repository.VerificationChallengeRepository;
import com.tradebridge.validation.email.EmailValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Email verification and activation (spec §3.1A).
 */
@Service
public class EmailVerificationService {

    private final UserRepository userRepository;
    private final VerificationChallengeRepository challengeRepository;
    private final IdentityAuditRecordRepository auditRepository;
    private final IdentityEventPublisher eventPublisher;
    private final IdentityProperties properties;
    private final SecurityNotificationHandoff notificationHandoff;

    public EmailVerificationService(UserRepository userRepository,
                                    VerificationChallengeRepository challengeRepository,
                                    IdentityAuditRecordRepository auditRepository,
                                    IdentityEventPublisher eventPublisher,
                                    IdentityProperties properties,
                                    SecurityNotificationHandoff notificationHandoff) {
        this.userRepository = userRepository;
        this.challengeRepository = challengeRepository;
        this.auditRepository = auditRepository;
        this.eventPublisher = eventPublisher;
        this.properties = properties;
        this.notificationHandoff = notificationHandoff;
    }

    @Transactional
    public void verifyEmail(VerifyEmailRequest request) {
        String email = EmailValidator.normalize(request.email()).value();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> ResourceNotFoundException.of("User", email));

        List<VerificationChallenge> active = challengeRepository
                .findActiveByUserAndType(user.getId(), ChallengeType.EMAIL_VERIFICATION, Instant.now());
        if (active.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_STATE,
                    "No active verification challenge; please request a new one");
        }
        VerificationChallenge challenge = active.get(0);

        if (challenge.getAttemptCount() >= properties.getVerification().getMaxAttempts()) {
            throw new BusinessException(ErrorCode.RATE_LIMIT_EXCEEDED,
                    "Too many attempts; request a new verification email");
        }

        String providedHash = RegistrationService.hashOtp(request.code());
        if (!providedHash.equals(challenge.getCodeHash())) {
            challenge.incrementAttempt();
            challengeRepository.save(challenge);
            throw new ValidationException("Invalid verification code",
                    List.of(new com.tradebridge.common.response.FieldError("code", "Invalid code")));
        }

        challenge.consume();
        challengeRepository.save(challenge);
        user.markEmailVerified();
        user.activate();
        userRepository.save(user);

        UUID correlationId = UUID.fromString(CorrelationContext.currentOrGenerate());
        eventPublisher.publishUserActivated(user.getId(), correlationId);
        audit(user.getId(), user.getId(), IdentityAction.USER_EMAIL_VERIFIED, Map.of());
        audit(user.getId(), user.getId(), IdentityAction.USER_ACTIVATED, Map.of());
    }

    @Transactional
    public void resendVerification(String email) {
        // Always return generic message — never disclose account existence
        if (email == null || email.isBlank()) {
            return;
        }
        String normalized = EmailValidator.normalize(email).value();
        userRepository.findByEmail(normalized).ifPresent(user -> {
            if (user.isEmailVerified() || user.getStatus() != UserStatus.PENDING_VERIFICATION) {
                return;
            }
            Instant last = challengeRepository
                    .lastIssuedAt(user.getId(), ChallengeType.EMAIL_VERIFICATION)
                    .orElse(Instant.EPOCH);
            if (last.plus(properties.getVerification().getResendCooldown()).isAfter(Instant.now())) {
                return; // rate-limited silently
            }
            UUID challengeId = RegistrationServiceSupport.issueEmailChallenge(
                    user, challengeRepository, properties);
            UUID correlationId = UUID.fromString(CorrelationContext.currentOrGenerate());
            notificationHandoff.requestEmailVerification(
                    user.getId(), challengeId,
                    com.tradebridge.common.util.StringUtils.maskEmail(user.getEmail()),
                    correlationId);
        });
    }

    @Transactional(readOnly = true)
    public Map<String, Object> currentUserStatus() {
        // Principal comes from security context; the controller wraps this
        // in a @PreAuthorize("isAuthenticated()") guard.
        var principal = com.tradebridge.security.context.SecurityContextUtils.currentPrincipal()
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "Not authenticated"));
        User user = userRepository.findById(principal.getUserId())
                .orElseThrow(() -> ResourceNotFoundException.of("User", principal.getUserId()));
        return Map.of(
                "userId", user.getId().toString(),
                "emailVerified", user.isEmailVerified(),
                "phoneVerified", user.isPhoneVerified(),
                "status", user.getStatus().name(),
                "mfaEnabled", user.isMfaEnabled()
        );
    }

    private void audit(UUID userId, UUID actorId, IdentityAction action, Map<String, Object> metadata) {
        auditRepository.save(new com.tradebridge.identity.domain.IdentityAuditRecord(
                userId, actorId, action, null, metadata));
    }

    /** Small helper mirroring RegistrationService's challenge creation for resend. */
    static final class RegistrationServiceSupport {
        private RegistrationServiceSupport() { }

        static UUID issueEmailChallenge(User user,
                                        VerificationChallengeRepository repo,
                                        IdentityProperties props) {
            String otp = RegistrationService.generateOtp();
            String hash = RegistrationService.hashOtp(otp);
            Instant expiry = Instant.now().plus(props.getVerification().getOtpTtl());
            VerificationChallenge challenge = new VerificationChallenge(
                    user.getId(), ChallengeType.EMAIL_VERIFICATION,
                    com.tradebridge.common.util.StringUtils.maskEmail(user.getEmail()),
                    hash, expiry);
            repo.save(challenge);
            // Register OTP for one-time delivery
            RegistrationService.ChallengeDeliveryRegistry.register(
                    challenge.getId(), otp, user.getEmail(), props.getVerification().getOtpTtl());
            return challenge.getId();
        }
    }
}
