package com.tradebridge.notification.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

/**
 * Optional internal-auth filter. Notification-service has no internal
 * endpoints at present, but the filter is included for symmetry with
 * the identity/customer services — future internal APIs (e.g. an
 * OTP delivery push endpoint) can reuse it.
 *
 * <p>If the incoming request carries {@code X-Internal-Secret} matching
 * {@code tradebridge.notification.internal-secret}, we set a synthetic
 * principal so that {@code /api/v1/notifications/internal/**} routes
 * can be allowed without a user JWT.
 */
@Component
public class InternalAuthFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Internal-Secret";
    public static final String CALLER_HEADER = "X-Caller-Service";

    private final byte[] expectedSecret;

    public InternalAuthFilter(@Value("${tradebridge.notification.internal-secret:}") String secret) {
        this.expectedSecret = (secret == null ? "" : secret).getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/v1/notifications/internal/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String provided = request.getHeader(HEADER);
        if (expectedSecret.length == 0 || provided == null
                || !MessageDigest.isEqual(expectedSecret, provided.getBytes(StandardCharsets.UTF_8))) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"success\":false,\"code\":\"UNAUTHORIZED\","
                    + "\"message\":\"Internal endpoint\"}");
            return;
        }
        String caller = request.getHeader(CALLER_HEADER);
        if (caller != null && !caller.isBlank()) {
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(caller, null,
                            List.of(new SimpleGrantedAuthority("SERVICE_INTERNAL"))));
        }
        try {
            chain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}