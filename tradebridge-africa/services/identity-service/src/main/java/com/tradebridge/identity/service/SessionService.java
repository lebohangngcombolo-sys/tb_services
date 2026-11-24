package com.tradebridge.identity.service;

import com.tradebridge.common.context.CorrelationContext;
import com.tradebridge.common.exception.BusinessException;
import com.tradebridge.common.exception.ErrorCode;
import com.tradebridge.common.exception.ResourceNotFoundException;
import com.tradebridge.common.id.UuidV7Generator;
import com.tradebridge.identity.api.dto.response.SessionResponse;
import com.tradebridge.identity.config.IdentityProperties;
import com.tradebridge.identity.domain.UserSession;
import com.tradebridge.identity.event.IdentityEventPublisher;
import com.tradebridge.identity.repository.RefreshTokenRepository;
import com.tradebridge.identity.repository.UserSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Session lifecycle (spec §3.1B, §3.2 {@code user_sessions}).
 *
 * <p>Enforces {@code max-active-per-user}: when a user logs in beyond the
 * cap, the oldest session is revoked and its event is published.
 */
@Service
public class SessionService {

    private final UserSessionRepository sessionRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final IdentityEventPublisher eventPublisher;
    private final IdentityProperties properties;

    public SessionService(UserSessionRepository sessionRepository,
                          RefreshTokenRepository refreshTokenRepository,
                          IdentityEventPublisher eventPublisher,
                          IdentityProperties properties) {
        this.sessionRepository = sessionRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.eventPublisher = eventPublisher;
        this.properties = properties;
    }

    @Transactional
    public UserSession createSession(UUID userId, String deviceName, String userAgent, String ip) {
        Instant now = Instant.now();
        List<UserSession> active = sessionRepository.findActiveByUserId(userId, now);
        if (active.size() >= properties.getSession().getMaxActivePerUser()) {
            UserSession oldest = active.get(0);
            revokeInternal(oldest, UUID.fromString(CorrelationContext.currentOrGenerate()));
        }

        UserSession session = new UserSession(
                UuidV7Generator.next(),
                userId,
                deviceName,
                ip,
                userAgent,
                now.plus(properties.getSession().getAbsoluteTtl()));
        return sessionRepository.save(session);
    }

    @Transactional(readOnly = true)
    public List<SessionResponse> listActiveSessions(UUID userId) {
        Instant now = Instant.now();
        return sessionRepository.findActiveByUserId(userId, now).stream()
                .map(s -> new SessionResponse(
                        s.getId(),
                        s.getDeviceName(),
                        maskIp(s.getIpAddress()),
                        s.getCreatedAt(),
                        s.getLastActivityAt(),
                        s.getExpiresAt(),
                        false))
                .toList();
    }

    @Transactional
    public void revokeSession(UUID sessionId, UUID userId) {
        UserSession session = sessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> ResourceNotFoundException.of("Session", sessionId));
        revokeInternal(session, UUID.fromString(CorrelationContext.currentOrGenerate()));
    }

    @Transactional
    public void revokeAllSessions(UUID userId) {
        Instant now = Instant.now();
        sessionRepository.revokeAllByUserId(userId, now);
        refreshTokenRepository.revokeAllByUserId(userId, now);
        eventPublisher.publishSessionRevoked(userId, null, UUID.fromString(
                CorrelationContext.currentOrGenerate()));
    }

    private void revokeInternal(UserSession session, UUID correlationId) {
        session.revoke();
        sessionRepository.save(session);
        refreshTokenRepository.revokeBySessionId(session.getId(), Instant.now());
        eventPublisher.publishSessionRevoked(session.getUserId(), session.getId(), correlationId);
    }

    private static String maskIp(String ip) {
        if (ip == null) return null;
        int dot = ip.lastIndexOf('.');
        return dot > 0 ? ip.substring(0, dot + 1) + "xxx" : ip;
    }
}
