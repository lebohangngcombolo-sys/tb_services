package com.tradebridge.identity.service;

import com.tradebridge.common.exception.BusinessException;
import com.tradebridge.common.exception.ErrorCode;
import com.tradebridge.common.exception.UnauthorizedException;
import com.tradebridge.identity.api.dto.response.TokenResponse;
import com.tradebridge.identity.config.IdentityProperties;
import com.tradebridge.identity.domain.RefreshToken;
import com.tradebridge.identity.domain.Role;
import com.tradebridge.identity.domain.User;
import com.tradebridge.identity.domain.UserRole;
import com.tradebridge.identity.domain.UserSession;
import com.tradebridge.identity.repository.RefreshTokenRepository;
import com.tradebridge.identity.repository.RoleRepository;
import com.tradebridge.identity.repository.UserRepository;
import com.tradebridge.identity.repository.UserRoleRepository;
import com.tradebridge.identity.repository.UserSessionRepository;
import com.tradebridge.security.config.SecurityProperties;
import com.tradebridge.security.jwt.JwtEncoderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
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
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Issues and rotates JWTs and refresh tokens (spec §3.1B, §3.4).
 *
 * <p>Refresh tokens: opaque 256-bit URL-safe values. Only the SHA-256
 * hash is stored. Rotation on use with reuse detection
 * ({@link #refresh(String)} — if a revoked token is presented, all
 * sessions for that user are revoked and the caller receives 401).
 */
@Service
public class TokenService {

    private static final Logger log = LoggerFactory.getLogger(TokenService.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final JwtEncoder jwtEncoder;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserSessionRepository sessionRepository;
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;
    private final IdentityProperties properties;
    private final SecurityProperties.Jwt jwtProperties;

    public TokenService(JwtEncoderFactory encoderFactory,
                        RefreshTokenRepository refreshTokenRepository,
                        UserSessionRepository sessionRepository,
                        UserRepository userRepository,
                        UserRoleRepository userRoleRepository,
                        RoleRepository roleRepository,
                        IdentityProperties properties,
                        SecurityProperties securityProperties) {
        this.jwtEncoder = encoderFactory.createEncoder();
        this.refreshTokenRepository = refreshTokenRepository;
        this.sessionRepository = sessionRepository;
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
        this.roleRepository = roleRepository;
        this.properties = properties;
        this.jwtProperties = securityProperties.getJwt();
    }

    @Transactional
    public TokenResponse issueTokens(User user, UserSession session) {
        Instant now = Instant.now();
        Instant accessExpiry = now.plus(properties.getAccessToken().getTtl());
        List<String> roleCodes = activeRoleCodes(user.getId(), now);

        String accessToken = signAccessToken(user, session, roleCodes, now, accessExpiry);

        // Issue refresh token
        String rawRefresh = generateOpaqueToken();
        Instant refreshExpiry = now.plus(properties.getRefreshToken().getTtl());
        RefreshToken stored = new RefreshToken(
                user.getId(), sha256Hex(rawRefresh), session.getId(), refreshExpiry);
        refreshTokenRepository.save(stored);

        return new TokenResponse(
                accessToken,
                "Bearer",
                properties.getAccessToken().getTtl().toSeconds(),
                rawRefresh,
                properties.getRefreshToken().getTtl().toSeconds(),
                false,
                null,
                new TokenResponse.UserSummary(
                        user.getId().toString(),
                        user.getEmail(),
                        user.getFirstName(),
                        user.getLastName(),
                        roleCodes));
    }

    /**
     * Called when the user has MFA enabled. Returns a short-lived MFA
     * challenge reference instead of access + refresh tokens.
     */
    @Transactional
    public TokenResponse mfaChallengeResponse(User user, UserSession session) {
        return new TokenResponse(
                null,
                "Bearer",
                0L,
                null,
                0L,
                true,
                session.getId().toString(),
                new TokenResponse.UserSummary(
                        user.getId().toString(),
                        user.getEmail(),
                        user.getFirstName(),
                        user.getLastName(),
                        activeRoleCodes(user.getId(), Instant.now())));
    }

    @Transactional
    public TokenResponse refresh(String rawRefreshToken) {
        String hash = sha256Hex(rawRefreshToken);
        RefreshToken stored = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new UnauthorizedException(ErrorCode.TOKEN_INVALID,
                        "Refresh token not recognised"));

        Instant now = Instant.now();
        if (stored.getRevokedAt() != null) {
            // Reuse detection: token was already used/revoked.
            log.warn("Refresh token reuse detected for user {}", stored.getUserId());
            sessionRepository.revokeAllByUserId(stored.getUserId(), now);
            refreshTokenRepository.revokeAllByUserId(stored.getUserId(), now);
            throw new UnauthorizedException(ErrorCode.TOKEN_INVALID,
                    "Refresh token reuse detected; all sessions revoked");
        }
        if (!now.isBefore(stored.getExpiresAt())) {
            throw new UnauthorizedException(ErrorCode.TOKEN_EXPIRED, "Refresh token expired");
        }

        UserSession session = sessionRepository.findById(stored.getSessionId())
                .orElseThrow(() -> new UnauthorizedException(ErrorCode.TOKEN_INVALID,
                        "Session no longer exists"));
        if (!session.isActiveAt(now)) {
            throw new UnauthorizedException(ErrorCode.TOKEN_INVALID, "Session is no longer active");
        }

        // Rotate: revoke old, issue new
        stored.revoke();
        refreshTokenRepository.save(stored);

        User user = userRepository.findById(stored.getUserId())
                .orElseThrow(() -> new UnauthorizedException(ErrorCode.TOKEN_INVALID,
                        "User no longer exists"));

        return issueTokens(user, session);
    }

    /**
     * Builds a signed JWT for the given user + session. Includes roles
     * and standard claims but never personal data (spec §3.4).
     */
    private String signAccessToken(User user, UserSession session, List<String> roleCodes,
                                   Instant issuedAt, Instant expiresAt) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(jwtProperties.getIssuer())
                .audience(List.of(jwtProperties.getAudience()))
                .subject(user.getId().toString())
                .issuedAt(issuedAt)
                .notBefore(issuedAt)
                .expiresAt(expiresAt)
                .id(UUID.randomUUID().toString())
                .claim("sid", session.getId().toString())
                .claim("email", user.getEmail())
                .claim("roles", roleCodes)
                .claim("ttyp", "user")
                .build();

        var parameters = JwtEncoderParameters.from(claims);
        return jwtEncoder.encode(parameters).getTokenValue();
    }

    private List<String> activeRoleCodes(UUID userId, Instant now) {
        List<UserRole> active = userRoleRepository.findActiveByUserId(userId, now);
        Set<UUID> roleIds = active.stream().map(UserRole::getRoleId).collect(Collectors.toSet());
        if (roleIds.isEmpty()) {
            return List.of();
        }
        return roleRepository.findAllById(roleIds).stream()
                .map(Role::getCode)
                .toList();
    }

    private static String generateOpaqueToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
