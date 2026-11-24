package com.tradebridge.identity.service;

import com.tradebridge.common.context.CorrelationContext;
import com.tradebridge.common.exception.BusinessException;
import com.tradebridge.common.exception.ErrorCode;
import com.tradebridge.common.exception.ResourceNotFoundException;
import com.tradebridge.common.exception.ValidationException;
import com.tradebridge.identity.api.dto.request.ChangePasswordRequest;
import com.tradebridge.identity.api.dto.request.ForgotPasswordRequest;
import com.tradebridge.identity.api.dto.request.ResetPasswordRequest;
import com.tradebridge.identity.config.IdentityProperties;
import com.tradebridge.identity.domain.IdentityAuditRecord;
import com.tradebridge.identity.domain.PasswordResetToken;
import com.tradebridge.identity.domain.User;
import com.tradebridge.identity.domain.UserCredential;
import com.tradebridge.identity.domain.enums.IdentityAction;
import com.tradebridge.identity.event.IdentityEventPublisher;
import com.tradebridge.identity.repository.IdentityAuditRecordRepository;
import com.tradebridge.identity.repository.PasswordResetTokenRepository;
import com.tradebridge.identity.repository.RefreshTokenRepository;
import com.tradebridge.identity.repository.UserCredentialRepository;
import com.tradebridge.identity.repository.UserRepository;
import com.tradebridge.identity.repository.UserSessionRepository;
import com.tradebridge.validation.email.EmailValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Password change, reset request and reset completion (spec §3.1C, §3.4).
 */
@Service
public class PasswordService {

    private static final Logger log = LoggerFactory.getLogger(PasswordService.class);
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final java.time.Duration RESET_TTL = java.time.Duration.ofHours(1);

    private final UserRepository userRepository;
    private final UserCredentialRepository credentialRepository;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final UserSessionRepository sessionRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final IdentityAuditRecordRepository auditRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicyEvaluator passwordPolicy;
    private final IdentityEventPublisher eventPublisher;
    private final SecurityNotificationHandoff notificationHandoff;
    private final IdentityProperties properties;

    public PasswordService(UserRepository userRepository,
                           UserCredentialRepository credentialRepository,
                           PasswordResetTokenRepository resetTokenRepository,
                           UserSessionRepository sessionRepository,
                           RefreshTokenRepository refreshTokenRepository,
                           IdentityAuditRecordRepository auditRepository,
                           PasswordEncoder passwordEncoder,
                           PasswordPolicyEvaluator passwordPolicy,
                           IdentityEventPublisher eventPublisher,
                           SecurityNotificationHandoff notificationHandoff,
                           IdentityProperties properties) {
        this.userRepository = userRepository;
        this.credentialRepository = credentialRepository;
        this.resetTokenRepository = resetTokenRepository;
        this.sessionRepository = sessionRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.auditRepository = auditRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordPolicy = passwordPolicy;
        this.eventPublisher = eventPublisher;
        this.notificationHandoff = notificationHandoff;
        this.properties = properties;
    }

    @Transactional
    public void requestReset(ForgotPasswordRequest request) {
        if (request.email() == null || request.email().isBlank()) {
            return; // silently ignored
        }
        String email = EmailValidator.normalize(request.email()).value();
        userRepository.findByEmail(email).ifPresent(user -> {
            String raw = generateOpaqueToken();
            Instant expiry = Instant.now().plus(RESET_TTL);
            resetTokenRepository.save(new PasswordResetToken(user.getId(), sha256Hex(raw), expiry));

            // Register for one-time delivery, same mechanism as OTP
            RegistrationService.ChallengeDeliveryRegistry.register(
                    deriveHandoffKey(user.getId()), raw, user.getEmail(), RESET_TTL);

            UUID correlationId = UUID.fromString(CorrelationContext.currentOrGenerate());
            notificationHandoff.requestPasswordReset(
                    user.getId(), deriveHandoffKey(user.getId()),
                    com.tradebridge.common.util.StringUtils.maskEmail(user.getEmail()),
                    correlationId);
            audit(user.getId(), user.getId(), IdentityAction.PASSWORD_RESET_REQUESTED, Map.of());
        });
    }

    @Transactional
    public void reset(ResetPasswordRequest request) {
        String hash = sha256Hex(request.token());
        PasswordResetToken token = resetTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new BusinessException(ErrorCode.TOKEN_INVALID,
                        "Password reset token is invalid"));
        if (!token.isUsableAt(Instant.now())) {
            throw new BusinessException(ErrorCode.TOKEN_EXPIRED, "Password reset token has expired");
        }
        passwordPolicy.evaluate(request.newPassword());

        UserCredential creds = credentialRepository.findByUserId(token.getUserId())
                .orElseThrow(() -> ResourceNotFoundException.of("UserCredential", token.getUserId()));
        creds.updatePassword(passwordEncoder.encode(request.newPassword()));
        credentialRepository.save(creds);

        token.consume();
        resetTokenRepository.save(token);
        resetTokenRepository.consumeAllForUser(token.getUserId(), Instant.now());

        // Invalidate all sessions on password reset
        Instant now = Instant.now();
        sessionRepository.revokeAllByUserId(token.getUserId(), now);
        refreshTokenRepository.revokeAllByUserId(token.getUserId(), now);

        UUID correlationId = UUID.fromString(CorrelationContext.currentOrGenerate());
        eventPublisher.publishPasswordChanged(token.getUserId(), correlationId);
        audit(token.getUserId(), token.getUserId(),
                IdentityAction.PASSWORD_RESET_COMPLETED, Map.of());
    }

    @Transactional
    public void change(UUID userId, UUID currentSessionId, ChangePasswordRequest request) {
        UserCredential creds = credentialRepository.findByUserId(userId)
                .orElseThrow(() -> ResourceNotFoundException.of("UserCredential", userId));
        if (!passwordEncoder.matches(request.currentPassword(), creds.getPasswordHash())) {
            throw new ValidationException("Current password is incorrect",
                    List.of(new com.tradebridge.common.response.FieldError(
                            "currentPassword", "Current password is incorrect")));
        }
        passwordPolicy.evaluate(request.newPassword());
        creds.updatePassword(passwordEncoder.encode(request.newPassword()));
        credentialRepository.save(creds);

        // Revoke all other sessions; keep current
        Instant now = Instant.now();
        sessionRepository.findActiveByUserId(userId, now).stream()
                .filter(s -> !s.getId().equals(currentSessionId))
                .forEach(s -> {
                    s.revoke();
                    sessionRepository.save(s);
                });

        UUID correlationId = UUID.fromString(CorrelationContext.currentOrGenerate());
        eventPublisher.publishPasswordChanged(userId, correlationId);
        audit(userId, userId, IdentityAction.PASSWORD_CHANGED, Map.of());
    }

    @Transactional(readOnly = true)
    public boolean validateCurrentPassword(UUID userId, String rawPassword) {
        if (rawPassword == null || rawPassword.isBlank()) {
            return false;
        }
        return credentialRepository.findByUserId(userId)
                .map(c -> passwordEncoder.matches(rawPassword, c.getPasswordHash()))
                .orElse(false);
    }

    private void audit(UUID userId, UUID actorId, IdentityAction action, Map<String, Object> metadata) {
        auditRepository.save(new IdentityAuditRecord(userId, actorId, action, null, metadata));
    }

    private static String generateOpaqueToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String sha256Hex(String value) {
        try {
            MessageDigest d = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(d.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Deterministic UUID so the delivery key can be looked up without storing the raw value. */
    private static UUID deriveHandoffKey(UUID userId) {
        return UUID.nameUUIDFromBytes(("password-reset:" + userId).getBytes(StandardCharsets.UTF_8));
    }
}
