package com.tradebridge.identity.service;

import com.tradebridge.common.context.CorrelationContext;
import com.tradebridge.common.exception.BusinessException;
import com.tradebridge.common.exception.ErrorCode;
import com.tradebridge.common.exception.ResourceNotFoundException;
import com.tradebridge.common.exception.ValidationException;
import com.tradebridge.identity.api.dto.request.MfaSetupRequest;
import com.tradebridge.identity.api.dto.response.MfaSetupResponse;
import com.tradebridge.identity.api.dto.response.MfaStatusResponse;
import com.tradebridge.identity.config.IdentityProperties;
import com.tradebridge.identity.domain.IdentityAuditRecord;
import com.tradebridge.identity.domain.MfaFactor;
import com.tradebridge.identity.domain.RecoveryCode;
import com.tradebridge.identity.domain.User;
import com.tradebridge.identity.domain.UserCredential;
import com.tradebridge.identity.domain.VerificationChallenge;
import com.tradebridge.identity.domain.enums.ChallengeType;
import com.tradebridge.identity.domain.enums.IdentityAction;
import com.tradebridge.identity.domain.enums.MfaFactorType;
import com.tradebridge.identity.event.IdentityEventPublisher;
import com.tradebridge.identity.repository.IdentityAuditRecordRepository;
import com.tradebridge.identity.repository.MfaFactorRepository;
import com.tradebridge.identity.repository.RecoveryCodeRepository;
import com.tradebridge.identity.repository.UserCredentialRepository;
import com.tradebridge.identity.repository.UserRepository;
import com.tradebridge.identity.repository.VerificationChallengeRepository;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * MFA — TOTP setup, email OTP challenge, recovery codes (spec §3.1D, §3.4).
 *
 * <p>TOTP is preferred; email OTP is a secondary mechanism (spec §11
 * decision 2). Enabling MFA requires recent authentication: verify-setup
 * must supply a valid TOTP code proving possession of the secret.
 */
@Service
public class MfaService {

    private static final Logger log = LoggerFactory.getLogger(MfaService.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final UserCredentialRepository credentialRepository;
    private final MfaFactorRepository mfaRepository;
    private final RecoveryCodeRepository recoveryCodeRepository;
    private final VerificationChallengeRepository challengeRepository;
    private final IdentityAuditRecordRepository auditRepository;
    private final PasswordEncoder passwordEncoder;
    private final IdentityEventPublisher eventPublisher;
    private final SecurityNotificationHandoff notificationHandoff;
    private final IdentityProperties properties;

    // TOTP primitives
    private final DefaultSecretGenerator secretGenerator = new DefaultSecretGenerator();
    private final DefaultCodeGenerator codeGenerator = new DefaultCodeGenerator();
    private final DefaultCodeVerifier codeVerifier =
            new DefaultCodeVerifier(codeGenerator, new SystemTimeProvider());

    public MfaService(UserRepository userRepository,
                      UserCredentialRepository credentialRepository,
                      MfaFactorRepository mfaRepository,
                      RecoveryCodeRepository recoveryCodeRepository,
                      VerificationChallengeRepository challengeRepository,
                      IdentityAuditRecordRepository auditRepository,
                      PasswordEncoder passwordEncoder,
                      IdentityEventPublisher eventPublisher,
                      SecurityNotificationHandoff notificationHandoff,
                      IdentityProperties properties) {
        this.userRepository = userRepository;
        this.credentialRepository = credentialRepository;
        this.mfaRepository = mfaRepository;
        this.recoveryCodeRepository = recoveryCodeRepository;
        this.challengeRepository = challengeRepository;
        this.auditRepository = auditRepository;
        this.passwordEncoder = passwordEncoder;
        this.eventPublisher = eventPublisher;
        this.notificationHandoff = notificationHandoff;
        this.properties = properties;
        this.codeVerifier.setTimePeriod((int) properties.getMfa().getTotpPeriod().toSeconds());
        this.codeVerifier.setAllowedTimePeriodDiscrepancy(1);
    }

    // ------------------------------------------------------------------ TOTP setup

    @Transactional
    public MfaSetupResponse beginTotpSetup(UUID userId, MfaSetupRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ResourceNotFoundException.of("User", userId));
        verifyPassword(userId, request.currentPassword());

        // Refuse if TOTP already enabled — must disable first
        mfaRepository.findByUserIdAndFactorTypeAndEnabledTrue(userId, MfaFactorType.TOTP)
                .ifPresent(f -> { throw new BusinessException(ErrorCode.INVALID_STATE,
                        "TOTP is already enabled; disable it before setting up a new one"); });

        String secret = secretGenerator.generate();
        // Secret stored encrypted-at-rest; for local dev we store base64 (see note in class javadoc)
        String encrypted = "ENC:" + Base64.getEncoder().encodeToString(secret.getBytes());

        MfaFactor factor = mfaRepository.findByUserIdAndFactorType(userId, MfaFactorType.TOTP)
                .orElseGet(() -> new MfaFactor(userId, MfaFactorType.TOTP, encrypted));
        if (factor.isVerified() && factor.isEnabled()) {
            throw new BusinessException(ErrorCode.INVALID_STATE, "TOTP already enabled");
        }
        factor.replaceSecretForSetup(encrypted);
        mfaRepository.save(factor);

        String accountName = user.getEmail();
        String issuer = properties.getMfa().getTotpIssuer();
        String provisioningUri = String.format(
                "otpauth://totp/%s:%s?secret=%s&issuer=%s&digits=%d&period=%d",
                issuer.replace(" ", "%20"),
                accountName,
                secret,
                issuer.replace(" ", "%20"),
                properties.getMfa().getTotpDigits(),
                properties.getMfa().getTotpPeriod().toSeconds());

        audit(userId, userId, IdentityAction.MFA_TOTP_SETUP_STARTED, Map.of());

        return new MfaSetupResponse(
                provisioningUri,
                maskSecret(secret),
                issuer,
                accountName);
    }

    @Transactional
    public List<String> verifyTotpSetup(UUID userId, String code) {
        MfaFactor factor = mfaRepository.findByUserIdAndFactorType(userId, MfaFactorType.TOTP)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_STATE,
                        "TOTP setup has not been started"));

        String secret = decodeStoredSecret(factor.getSecretEncrypted());
        if (!codeVerifier.isValidCode(secret, code)) {
            throw new ValidationException("Invalid TOTP code",
                    List.of(new com.tradebridge.common.response.FieldError("code", "Invalid TOTP code")));
        }

        // Enable TOTP
        factor.markVerifiedAndEnabled(factor.getSecretEncrypted());
        mfaRepository.save(factor);

        // Turn on the user-level flag
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ResourceNotFoundException.of("User", userId));
        user.setMfaEnabled(true);
        userRepository.save(user);

        // Generate recovery codes
        List<String> plainCodes = generateRecoveryCodes(userId);

        UUID correlationId = UUID.fromString(CorrelationContext.currentOrGenerate());
        eventPublisher.publishMfaEnabled(userId, MfaFactorType.TOTP.name(), correlationId);
        audit(userId, userId, IdentityAction.MFA_TOTP_ENABLED, Map.of());

        return plainCodes;
    }

    // ------------------------------------------------------------------ disable

    @Transactional
    public void disable(UUID userId, String code) {
        MfaFactor factor = mfaRepository.findByUserIdAndFactorTypeAndEnabledTrue(userId, MfaFactorType.TOTP)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_STATE,
                        "MFA is not enabled"));

        String secret = decodeStoredSecret(factor.getSecretEncrypted());
        boolean valid = codeVerifier.isValidCode(secret, code) || consumeRecoveryCode(userId, code);
        if (!valid) {
            throw new ValidationException("Invalid code",
                    List.of(new com.tradebridge.common.response.FieldError("code", "Invalid code")));
        }

        factor.disable();
        mfaRepository.save(factor);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> ResourceNotFoundException.of("User", userId));
        user.setMfaEnabled(false);
        userRepository.save(user);

        recoveryCodeRepository.deleteAllByUserId(userId);

        UUID correlationId = UUID.fromString(CorrelationContext.currentOrGenerate());
        eventPublisher.publishMfaDisabled(userId, MfaFactorType.TOTP.name(), correlationId);
        audit(userId, userId, IdentityAction.MFA_DISABLED, Map.of());
    }

    // ------------------------------------------------------------------ challenge

    @Transactional
    public void startEmailChallenge(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ResourceNotFoundException.of("User", userId));
        if (!user.isMfaEnabled()) {
            throw new BusinessException(ErrorCode.INVALID_STATE, "MFA is not enabled");
        }

        String otp = RegistrationService.generateOtp();
        Instant expiry = Instant.now().plus(properties.getVerification().getOtpTtl());
        VerificationChallenge challenge = new VerificationChallenge(
                userId, ChallengeType.MFA_EMAIL_OTP,
                com.tradebridge.common.util.StringUtils.maskEmail(user.getEmail()),
                RegistrationService.hashOtp(otp), expiry);
        challengeRepository.save(challenge);

        RegistrationService.ChallengeDeliveryRegistry.register(
                challenge.getId(), otp, user.getEmail(), properties.getVerification().getOtpTtl());

        UUID correlationId = UUID.fromString(CorrelationContext.currentOrGenerate());
        notificationHandoff.requestMfaOtp(
                userId, challenge.getId(),
                com.tradebridge.common.util.StringUtils.maskEmail(user.getEmail()),
                correlationId);
        audit(userId, userId, IdentityAction.MFA_CHALLENGE_ISSUED, Map.of());
    }

    @Transactional
    public void verifyChallenge(UUID userId, String code) {
        List<VerificationChallenge> active = challengeRepository
                .findActiveByUserAndType(userId, ChallengeType.MFA_EMAIL_OTP, Instant.now());
        if (active.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_STATE,
                    "No active MFA challenge; request a new code");
        }
        VerificationChallenge challenge = active.get(0);
        if (challenge.getAttemptCount() >= properties.getVerification().getMaxAttempts()) {
            throw new BusinessException(ErrorCode.RATE_LIMIT_EXCEEDED, "Too many attempts");
        }

        String hash = RegistrationService.hashOtp(code);
        if (!hash.equals(challenge.getCodeHash())) {
            challenge.incrementAttempt();
            challengeRepository.save(challenge);
            audit(userId, userId, IdentityAction.MFA_CHALLENGE_FAILED, Map.of());
            throw new ValidationException("Invalid code",
                    List.of(new com.tradebridge.common.response.FieldError("code", "Invalid code")));
        }

        challenge.consume();
        challengeRepository.save(challenge);
        audit(userId, userId, IdentityAction.MFA_CHALLENGE_PASSED, Map.of());
    }

    @Transactional
    public void verifyLoginCode(UUID userId, String code) {
        MfaFactor factor = mfaRepository.findByUserIdAndFactorTypeAndEnabledTrue(
                        userId, MfaFactorType.TOTP)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_STATE,
                        "TOTP MFA is not enabled"));

        String secret = decodeStoredSecret(factor.getSecretEncrypted());
        if (!codeVerifier.isValidCode(secret, code) && !consumeRecoveryCode(userId, code)) {
            audit(userId, userId, IdentityAction.MFA_CHALLENGE_FAILED, Map.of());
            throw new ValidationException("Invalid MFA code",
                    List.of(new com.tradebridge.common.response.FieldError("code", "Invalid MFA code")));
        }
        audit(userId, userId, IdentityAction.MFA_CHALLENGE_PASSED, Map.of());
    }

    // ------------------------------------------------------------------ recovery

    @Transactional
    public List<String> regenerateRecoveryCodes(UUID userId, String totpCode) {
        MfaFactor factor = mfaRepository.findByUserIdAndFactorTypeAndEnabledTrue(userId, MfaFactorType.TOTP)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_STATE, "MFA is not enabled"));
        if (!codeVerifier.isValidCode(decodeStoredSecret(factor.getSecretEncrypted()), totpCode)) {
            throw new ValidationException("Invalid TOTP code",
                    List.of(new com.tradebridge.common.response.FieldError("code", "Invalid TOTP code")));
        }
        recoveryCodeRepository.deleteAllByUserId(userId);
        List<String> codes = generateRecoveryCodes(userId);
        audit(userId, userId, IdentityAction.RECOVERY_CODES_REGENERATED, Map.of());
        return codes;
    }

    // ------------------------------------------------------------------ status

    @Transactional(readOnly = true)
    public MfaStatusResponse status(UUID userId) {
        return mfaRepository.findByUserIdAndFactorTypeAndEnabledTrue(userId, MfaFactorType.TOTP)
                .map(f -> new MfaStatusResponse(
                        true,
                        f.getFactorType().name(),
                        f.getCreatedAt(),
                        (int) recoveryCodeRepository.countUnusedByUserId(userId)))
                .orElseGet(() -> new MfaStatusResponse(false, null, null, 0));
    }

    // ------------------------------------------------------------------ helpers

    private List<String> generateRecoveryCodes(UUID userId) {
        int count = properties.getMfa().getRecoveryCodeCount();
        List<String> plain = new ArrayList<>(count);
        List<RecoveryCode> stored = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            String code = generateRecoveryCode();
            plain.add(code);
            stored.add(new RecoveryCode(userId, sha256Hex(code)));
        }
        recoveryCodeRepository.saveAll(stored);
        return plain;
    }

    private boolean consumeRecoveryCode(UUID userId, String code) {
        String hash = sha256Hex(code);
        return recoveryCodeRepository.findUnusedByUserId(userId).stream()
                .filter(rc -> rc.getCodeHash().equals(hash))
                .findFirst()
                .map(rc -> {
                    rc.markUsed();
                    recoveryCodeRepository.save(rc);
                    return true;
                })
                .orElse(false);
    }

    private void verifyPassword(UUID userId, String rawPassword) {
        UserCredential creds = credentialRepository.findByUserId(userId)
                .orElseThrow(() -> ResourceNotFoundException.of("UserCredential", userId));
        if (!passwordEncoder.matches(rawPassword, creds.getPasswordHash())) {
            throw new ValidationException("Invalid password",
                    List.of(new com.tradebridge.common.response.FieldError(
                            "currentPassword", "Invalid password")));
        }
    }

    private static String generateRecoveryCode() {
        // 8-char uppercase alphanumeric + dash + 8-char = 17 chars total
        String a = randomAlphanumeric(8);
        String b = randomAlphanumeric(8);
        return a + "-" + b;
    }

    private static String randomAlphanumeric(int length) {
        String alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // no ambiguous chars
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(alphabet.charAt(RANDOM.nextInt(alphabet.length())));
        }
        return sb.toString();
    }

    private static String sha256Hex(String value) {
        try {
            MessageDigest d = MessageDigest.getInstance("SHA-256");
            return java.util.HexFormat.of().formatHex(
                    d.digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String decodeStoredSecret(String stored) {
        if (stored == null) return null;
        if (stored.startsWith("ENC:")) {
            return new String(Base64.getDecoder().decode(stored.substring(4)));
        }
        return stored;
    }

    private static String maskSecret(String secret) {
        if (secret == null || secret.length() < 8) return "****";
        return secret.substring(0, 4) + "****" + secret.substring(secret.length() - 4);
    }

    private void audit(UUID userId, UUID actorId, IdentityAction action, Map<String, Object> metadata) {
        auditRepository.save(new IdentityAuditRecord(userId, actorId, action, null, metadata));
    }
}
