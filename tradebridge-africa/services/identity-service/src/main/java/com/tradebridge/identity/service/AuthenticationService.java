package com.tradebridge.identity.service;

import com.tradebridge.common.context.CorrelationContext;
import com.tradebridge.common.exception.BusinessException;
import com.tradebridge.common.exception.ErrorCode;
import com.tradebridge.common.exception.UnauthorizedException;
import com.tradebridge.identity.api.dto.request.LoginRequest;
import com.tradebridge.identity.api.dto.response.TokenResponse;
import com.tradebridge.identity.api.dto.response.UserProfileResponse;
import com.tradebridge.identity.config.IdentityProperties;
import com.tradebridge.identity.domain.IdentityAuditRecord;
import com.tradebridge.identity.domain.User;
import com.tradebridge.identity.domain.UserCredential;
import com.tradebridge.identity.domain.UserRole;
import com.tradebridge.identity.domain.Role;
import com.tradebridge.identity.domain.UserSession;
import com.tradebridge.identity.domain.enums.IdentityAction;
import com.tradebridge.identity.domain.enums.UserStatus;
import com.tradebridge.identity.mapper.UserMapper;
import com.tradebridge.identity.repository.IdentityAuditRecordRepository;
import com.tradebridge.identity.repository.RoleRepository;
import com.tradebridge.identity.repository.UserCredentialRepository;
import com.tradebridge.identity.repository.UserRepository;
import com.tradebridge.identity.repository.UserRoleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Password authentication, lockout enforcement, session creation (spec §3.1B, §3.4).
 *
 * <p>Rate limiting is enforced by the Gateway for public endpoints; the
 * database-backed lockout here is the second layer of defense.
 */
@Service
public class AuthenticationService {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationService.class);

    private final UserRepository userRepository;
    private final UserCredentialRepository credentialRepository;
    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;
    private final IdentityAuditRecordRepository auditRepository;
    private final PasswordEncoder passwordEncoder;
    private final SessionService sessionService;
    private final TokenService tokenService;
    private final MfaService mfaService;
    private final UserMapper userMapper;
    private final IdentityProperties properties;

    public AuthenticationService(UserRepository userRepository,
                                 UserCredentialRepository credentialRepository,
                                 UserRoleRepository userRoleRepository,
                                 RoleRepository roleRepository,
                                 IdentityAuditRecordRepository auditRepository,
                                 PasswordEncoder passwordEncoder,
                                 SessionService sessionService,
                                 TokenService tokenService,
                                 MfaService mfaService,
                                 UserMapper userMapper,
                                 IdentityProperties properties) {
        this.userRepository = userRepository;
        this.credentialRepository = credentialRepository;
        this.userRoleRepository = userRoleRepository;
        this.roleRepository = roleRepository;
        this.auditRepository = auditRepository;
        this.passwordEncoder = passwordEncoder;
        this.sessionService = sessionService;
        this.tokenService = tokenService;
        this.mfaService = mfaService;
        this.userMapper = userMapper;
        this.properties = properties;
    }

    @Transactional
    public TokenResponse login(LoginRequest request) {
        User user = findByLoginIdentifier(request.identifier())
                .orElseThrow(() -> new UnauthorizedException(ErrorCode.INVALID_CREDENTIALS,
                        "Invalid credentials"));

        UserCredential creds = credentialRepository.findByUserId(user.getId())
                .orElseThrow(() -> new UnauthorizedException(ErrorCode.INVALID_CREDENTIALS,
                        "Invalid credentials"));

        // Lockout check first
        Instant now = Instant.now();
        if (creds.isLockedAt(now)) {
            audit(user.getId(), user.getId(), IdentityAction.USER_LOGIN_FAILURE,
                    Map.of("reason", "locked"));
            throw new BusinessException(ErrorCode.ACCOUNT_LOCKED,
                    "Account temporarily locked; try again later");
        }

        // Status check
        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new BusinessException(ErrorCode.ACCOUNT_SUSPENDED, "Account is suspended");
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.INVALID_STATE,
                    "Account is not active: " + user.getStatus());
        }

        // Password check
        if (!passwordEncoder.matches(request.password(), creds.getPasswordHash())) {
            creds.recordFailedAttempt(
                    properties.getLockout().getThreshold(),
                    properties.getLockout().getLockDuration());
            credentialRepository.save(creds);
            audit(user.getId(), user.getId(), IdentityAction.USER_LOGIN_FAILURE,
                    Map.of("reason", "bad_credentials"));
            // If lockout just triggered, return locked message
            if (creds.isLockedAt(Instant.now())) {
                throw new BusinessException(ErrorCode.ACCOUNT_LOCKED,
                        "Account temporarily locked; try again later");
            }
            throw new UnauthorizedException(ErrorCode.INVALID_CREDENTIALS, "Invalid credentials");
        }

        // Success — reset lockout counter, create session, issue tokens
        creds.resetFailedAttempts();
        credentialRepository.save(creds);

        UserSession session = sessionService.createSession(
                user.getId(), request.deviceName(), request.userAgent(), null);

        // MFA challenge required?
        if (user.isMfaEnabled()) {
            audit(user.getId(), user.getId(), IdentityAction.USER_LOGIN_SUCCESS,
                    Map.of("mfaRequired", true));
            return tokenService.mfaChallengeResponse(user, session);
        }

        TokenResponse response = tokenService.issueTokens(user, session);
        audit(user.getId(), user.getId(), IdentityAction.USER_LOGIN_SUCCESS,
                Map.of("mfaRequired", false));
        return response;
    }

    @Transactional
    public TokenResponse completeMfaLogin(UUID challengeId, String code) {
        UserSession pendingSession = sessionService.findRecentActiveMfaSession(
                challengeId, properties.getMfa().getLoginChallengeTtl());
        User user = userRepository.findById(pendingSession.getUserId())
                .filter(User::isMfaEnabled)
                .orElseThrow(() -> new UnauthorizedException(ErrorCode.TOKEN_INVALID,
                        "MFA challenge is invalid or expired"));

        mfaService.verifyLoginCode(user.getId(), code);
        sessionService.revokeSession(pendingSession.getId(), user.getId());

        UserSession authenticatedSession = sessionService.createSession(
                user.getId(),
                pendingSession.getDeviceName(),
                pendingSession.getUserAgent(),
                pendingSession.getIpAddress());
        return tokenService.issueTokens(user, authenticatedSession);
    }

    @Transactional(readOnly = true)
    public UserProfileResponse currentUserProfile(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> com.tradebridge.common.exception.ResourceNotFoundException
                        .of("User", userId));
        List<String> roles = userRoleRepository
                .findActiveByUserId(userId, Instant.now())
                .stream()
                .map(ur -> roleRepository.findById(ur.getRoleId()).map(Role::getCode).orElse(null))
                .filter(java.util.Objects::nonNull)
                .toList();
        return userMapper.toProfile(user, roles);
    }

    private java.util.Optional<User> findByLoginIdentifier(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            return java.util.Optional.empty();
        }
        String trimmed = identifier.trim();
        if (trimmed.startsWith("+")) {
            return userRepository.findByPhoneNumber(trimmed);
        }
        return userRepository.findByEmail(trimmed.toLowerCase(java.util.Locale.ROOT));
    }

    private void audit(UUID userId, UUID actorId, IdentityAction action, Map<String, Object> metadata) {
        auditRepository.save(new IdentityAuditRecord(userId, actorId, action, null, metadata));
    }
}
