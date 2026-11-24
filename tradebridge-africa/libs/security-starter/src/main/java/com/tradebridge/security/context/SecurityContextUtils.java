package com.tradebridge.security.context;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Thread-local helpers for reading the current principal from a servlet
 * request context. Use inside controllers and services. Do <b>not</b> call
 * from reactive code (Gateway).
 */
public final class SecurityContextUtils {

    private SecurityContextUtils() { }

    public static Optional<AuthenticatedPrincipal> currentPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return Optional.empty();
        }
        if (!(auth instanceof JwtAuthenticationToken jwtAuth)) {
            return Optional.empty();
        }
        Jwt jwt = jwtAuth.getToken();
        Set<String> authorities = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        // Roles are typically prefixed with ROLE_; we normalize by keeping both
        Set<String> roles = authorities.stream()
                .filter(a -> !a.contains("."))
                .collect(Collectors.toSet());
        Set<String> permissions = authorities.stream()
                .filter(a -> a.contains("."))
                .collect(Collectors.toSet());

        UUID userId = parseUuid(jwt.getSubject());
        UUID sessionId = parseUuid(jwt.getClaimAsString("sid"));
        boolean serviceAccount = "service".equals(jwt.getClaimAsString("ttyp"));

        return Optional.of(new AuthenticatedPrincipal(
                userId, sessionId, roles, permissions, serviceAccount, jwt.getSubject()));
    }

    public static UUID requireUserId() {
        return currentPrincipal()
                .map(AuthenticatedPrincipal::getUserId)
                .orElseThrow(() -> new IllegalStateException("No authenticated principal"));
    }

    public static UUID currentUserIdOrNull() {
        return currentPrincipal().map(AuthenticatedPrincipal::getUserId).orElse(null);
    }

    public static boolean isAuthenticated() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.isAuthenticated()
                && !(auth instanceof org.springframework.security.authentication.AnonymousAuthenticationToken);
    }

    private static UUID parseUuid(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** Escape hatch for tests. */
    public static void clear() {
        SecurityContextHolder.clearContext();
    }

    public static Set<String> currentAuthorities() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return Collections.emptySet();
        }
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
    }
}
