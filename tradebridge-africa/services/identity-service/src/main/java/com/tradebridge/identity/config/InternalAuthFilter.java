package com.tradebridge.identity.config;

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
 * Authenticates internal-only endpoints via a shared secret header
 * (spec §3.3 "controlled notification handoff"). This is a deliberate
 * simplification: the spec's preferred model is a proper service
 * identity, which will be adopted when service-to-service auth is
 * formalized. The shared-secret model is safe as long as the secret is
 * rotated and only reachable within the service network.
 *
 * <p>Requests to {@code /api/v1/identity/internal/**} without the correct
 * header are rejected with 401.
 */
@Component
public class InternalAuthFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Internal-Secret";
    public static final String CALLER_HEADER = "X-Caller-Service";

    private final byte[] expectedSecret;

    public InternalAuthFilter(@Value("${tradebridge.identity.internal-secret:}") String secret) {
        this.expectedSecret = (secret == null ? "" : secret).getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/v1/identity/internal/");
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
                    new UsernamePasswordAuthenticationToken(
                            caller, null,
                            List.of(new SimpleGrantedAuthority("SERVICE_INTERNAL"))));
        }
        try {
            chain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
