package com.tradebridge.identity.mapper;

import com.tradebridge.identity.api.dto.response.SessionResponse;
import com.tradebridge.identity.domain.UserSession;

public final class SessionMapper {

    public SessionResponse toResponse(UserSession session, boolean current) {
        if (session == null) return null;
        return new SessionResponse(
                session.getId(),
                session.getDeviceName(),
                maskIp(session.getIpAddress()),
                session.getCreatedAt(),
                session.getLastActivityAt(),
                session.getExpiresAt(),
                current);
    }

    private static String maskIp(String ip) {
        if (ip == null) return null;
        int dot = ip.lastIndexOf('.');
        if (dot > 0) return ip.substring(0, dot + 1) + "xxx";
        int colon = ip.lastIndexOf(':');
        if (colon > 0) return ip.substring(0, colon + 1) + "xxxx";
        return ip;
    }
}
