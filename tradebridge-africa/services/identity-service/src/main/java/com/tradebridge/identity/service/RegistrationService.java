package com.tradebridge.identity.service;

import com.tradebridge.common.context.CorrelationContext;
import com.tradebridge.common.exception.ConflictException;
import com.tradebridge.common.exception.ErrorCode;
import com.tradebridge.common.exception.ResourceNotFoundException;
import com.tradebridge.common.id.UuidV7Generator;
import com.tradebridge.identity.api.dto.request.RegisterRequest;
import com.tradebridge.identity.api.dto.response.RegistrationResponse;
import com.tradebridge.identity.config.IdentityProperties;
import com.tradebridge.identity.domain.IdentityAuditRecord;
import com.tradebridge.identity.domain.User;
import com.tradebridge.identity.domain.UserCredential;
import com.tradebridge.identity.domain.UserRole;
import com.tradebridge.identity.domain.Role;
import com.tradebridge.identity.domain.VerificationChallenge;
import com.tradebridge.identity.domain.enums.ChallengeType;
import com.tradebridge.identity.domain.enums.IdentityAction;
import com.tradebridge.identity.domain.enums.MfaFactorType;
import com.tradebridge.identity.domain.enums.UserStatus;
import com.tradebridge.identity.event.IdentityEventPublisher;
import com.tradebridge.identity.repository.IdentityAuditRecordRepository;
import com.tradebridge.identity.repository.RoleRepository;
import com.tradebridge.identity.repository.UserCredentialRepository;
import com.tradebridge.identity.repository.UserRepository;
import com.tradebridge.identity.repository.UserRoleRepository;
import com.tradebridge.identity.repository.VerificationChallengeRepository;
import com.tradebridge.security.authorization.PermissionCode;
import com.tradebridge.validation.email.EmailValidator;
import com.tradebridge.validation.phone.E164PhoneValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

/**
 * Registers new users, creates the email-verification challenge and
 * publishes {@code identity.user.registered.v1} (spec §3.1A).
 *
 * <p>Does not create an organization — that belongs to Customer Service.
 */
@Service
public class RegistrationService {

    private static final Logger log = LoggerFactory.getLogger(RegistrationService.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final UserCredentialRepository credentialRepository;
    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;
    private final VerificationChallengeRepository challengeRepository;
    private final IdentityAuditRecordRepository auditRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicyEvaluator passwordPolicy;
    private final CountryValidationClient countryClient;
    private final SecurityNotificationHandoff notificationHandoff;
    private final IdentityEventPublisher eventPublisher;
    private final IdentityProperties properties;

    public RegistrationService(UserRepository userRepository,
                               UserCredentialRepository credentialRepository,
                               UserRoleRepository userRoleRepository,
                               RoleRepository roleRepository,
                               VerificationChallengeRepository challengeRepository,
                               IdentityAuditRecordRepository auditRepository,
                               PasswordEncoder passwordEncoder,
                               PasswordPolicyEvaluator passwordPolicy,
                               CountryValidationClient countryClient,
                               SecurityNotificationHandoff notificationHandoff,
                               IdentityEventPublisher eventPublisher,
                               IdentityProperties properties) {
        this.userRepository = userRepository;
        this.credentialRepository = credentialRepository;
        this.userRoleRepository = userRoleRepository;
        this.roleRepository = roleRepository;
        this.challengeRepository = challengeRepository;
        this.auditRepository = auditRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordPolicy = passwordPolicy;
        this.countryClient = countryClient;
        this.notificationHandoff = notificationHandoff;
        this.eventPublisher = eventPublisher;
        this.properties = properties;
    }

    @Transactional
    public RegistrationResponse register(RegisterRequest request) {
        String normalizedEmail = EmailValidator.normalize(request.email()).value();
        String normalizedPhone = E164PhoneValidator.normalize(request.phoneNumber());
        String country = request.countryCode().toUpperCase();
        String language = request.preferredLanguage().toUpperCase();

        // §3.4 validations
        passwordPolicy.evaluate(request.password());
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new ConflictException(ErrorCode.DUPLICATE_RESOURCE, "Email already registered");
        }
        if (userRepository.existsByPhoneNumber(normalizedPhone)) {
            throw new ConflictException(ErrorCode.DUPLICATE_RESOURCE, "Phone number already registered");
        }
        countryClient.requireEnabled(country);

        UUID userId = UuidV7Generator.next();
        User user = new User(userId,
                request.firstName().trim(),
                request.lastName().trim(),
                normalizedEmail,
                normalizedPhone,
                country,
                language);

        userRepository.save(user);
        credentialRepository.save(new UserCredential(userId, passwordEncoder.encode(request.password())));

        // Assign CUSTOMER role by default
        roleRepository.findByCode(PermissionCode.ROLE_CUSTOMER).ifPresent(role ->
                userRoleRepository.save(new UserRole(userId, role.getId(), null, null)));

        UUID challengeId = createVerificationChallenge(user, ChallengeType.EMAIL_VERIFICATION);
        audit(userId, userId, IdentityAction.USER_REGISTERED, Map.of("country", country));

        // Publish business events
        UUID correlationId = UUID.fromString(CorrelationContext.currentOrGenerate());
        eventPublisher.publishUserRegistered(userId, normalizedEmail, country, correlationId);
        notificationHandoff.requestEmailVerification(
                userId, challengeId, maskEmail(normalizedEmail), correlationId);

        log.info("Registered user {} in country {}", userId, country);

        return new RegistrationResponse(userId, normalizedEmail,
                UserStatus.PENDING_VERIFICATION.name(), true);
    }

    @Transactional
    public void activate(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ResourceNotFoundException.of("User", userId));
        if (user.getStatus() == UserStatus.ACTIVE) {
            return;
        }
        user.activate();
        userRepository.save(user);

        UUID correlationId = UUID.fromString(CorrelationContext.currentOrGenerate());
        eventPublisher.publishUserActivated(userId, correlationId);
        audit(userId, userId, IdentityAction.USER_ACTIVATED, Map.of());
    }

    private UUID createVerificationChallenge(User user, ChallengeType type) {
        String otp = generateOtp();
        String hash = hashOtp(otp);
        Instant expiry = Instant.now().plus(properties.getVerification().getOtpTtl());

        VerificationChallenge challenge = new VerificationChallenge(
                user.getId(), type, maskEmail(user.getEmail()), hash, expiry);
        challengeRepository.save(challenge);

        // Expose the raw OTP once via a short-lived in-memory handoff store.
        // The handoff store is implemented in Part 3b (ChallengeDeliveryStore).
        ChallengeDeliveryRegistry.register(challenge.getId(), otp, user.getEmail(),
                properties.getVerification().getOtpTtl());

        return challenge.getId();
    }

    private void audit(UUID userId, UUID actorId, IdentityAction action, Map<String, Object> metadata) {
        auditRepository.save(new IdentityAuditRecord(userId, actorId, action, null, metadata));
    }

    static String generateOtp() {
        int code = RANDOM.nextInt(1_000_000);
        return String.format("%06d", code);
    }

    static String hashOtp(String otp) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(otp.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private static String maskEmail(String email) {
        return com.tradebridge.common.util.StringUtils.maskEmail(email);
    }

    /**
     * Very small in-memory store for the raw OTP and its destination.
     * Consumed once by the internal {@code /challenge-delivery} callback that Notification
     * Service invokes. Not persisted (spec §3.3: never place raw OTPs in
     * Kafka payloads or logs). Entries expire after the challenge TTL.
     */
    public static final class ChallengeDeliveryRegistry {
    private static final java.util.concurrent.ConcurrentHashMap<UUID, Entry> STORE =
            new java.util.concurrent.ConcurrentHashMap<>();

    private ChallengeDeliveryRegistry() { }

    public static void register(UUID challengeId, String otp, String destination, Duration ttl) {
        STORE.put(challengeId, new Entry(otp, destination, Instant.now().plus(ttl)));
    }

    public static Entry consume(UUID challengeId) {
        Entry e = STORE.remove(challengeId);
        if (e == null || Instant.now().isAfter(e.expiresAt)) {
            return null;
        }
        return e;
    }

    public record Entry(String otp, String destination, Instant expiresAt) { }
}
}
