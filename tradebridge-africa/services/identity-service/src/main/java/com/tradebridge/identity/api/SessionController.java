package com.tradebridge.identity.api;

import com.tradebridge.common.response.ApiResponse;
import com.tradebridge.common.response.ResponseFactory;
import com.tradebridge.identity.api.dto.response.SessionResponse;
import com.tradebridge.identity.service.SessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Session management endpoints (spec §3.1B).
 */
@RestController
@RequestMapping("/api/v1/identity/sessions")
@Tag(name = "Sessions", description = "Active session management")
@PreAuthorize("isAuthenticated()")
public class SessionController {

    private final SessionService sessionService;

    public SessionController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @GetMapping
    @Operation(summary = "List active sessions of the current user")
    public ApiResponse<List<SessionResponse>> list(@AuthenticationPrincipal Jwt jwt) {
        List<SessionResponse> sessions = sessionService.listActiveSessions(UUID.fromString(jwt.getSubject()));
        return ResponseFactory.ok(sessions);
    }

    @DeleteMapping("/{sessionId}")
    @Operation(summary = "Revoke a specific session")
    public ApiResponse<Void> revoke(@AuthenticationPrincipal Jwt jwt,
                                    @PathVariable UUID sessionId) {
        sessionService.revokeSession(sessionId, UUID.fromString(jwt.getSubject()));
        return ResponseFactory.ok("Session revoked");
    }
}
