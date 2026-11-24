package com.tradebridge.identity.api;

import com.tradebridge.common.response.ApiResponse;
import com.tradebridge.common.response.ResponseFactory;
import com.tradebridge.identity.api.dto.request.LoginRequest;
import com.tradebridge.identity.api.dto.request.RefreshTokenRequest;
import com.tradebridge.identity.api.dto.response.TokenResponse;
import com.tradebridge.identity.api.dto.response.UserProfileResponse;
import com.tradebridge.identity.service.AuthenticationService;
import com.tradebridge.identity.service.SessionService;
import com.tradebridge.identity.service.TokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Authentication endpoints (spec §3.1B).
 *
 * <p>/login and /refresh are public (via Gateway allowlist). All others
 * require a valid JWT.
 */
@RestController
@RequestMapping("/api/v1/identity")
@Tag(name = "Authentication", description = "Login, token refresh, logout, and current user")
public class AuthController {

    private final AuthenticationService authenticationService;
    private final TokenService tokenService;
    private final SessionService sessionService;

    public AuthController(AuthenticationService authenticationService,
                          TokenService tokenService,
                          SessionService sessionService) {
        this.authenticationService = authenticationService;
        this.tokenService = tokenService;
        this.sessionService = sessionService;
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate with email/phone and password (public)")
    public ApiResponse<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        TokenResponse response = authenticationService.login(request);
        return ResponseFactory.ok(response, "Authentication successful");
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token (public; refresh token in body)")
    public ApiResponse<TokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        TokenResponse response = tokenService.refresh(request.refreshToken());
        return ResponseFactory.ok(response, "Token refreshed");
    }

    @PostMapping("/logout")
    @Operation(summary = "Revoke the current session")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> logout(@AuthenticationPrincipal Jwt jwt) {
        UUID sessionId = UUID.fromString(jwt.getClaimAsString("sid"));
        sessionService.revokeSession(sessionId, UUID.fromString(jwt.getSubject()));
        return ResponseFactory.ok("Logged out");
    }

    @PostMapping("/logout-all")
    @Operation(summary = "Revoke all sessions of the current user")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> logoutAll(@AuthenticationPrincipal Jwt jwt) {
        sessionService.revokeAllSessions(UUID.fromString(jwt.getSubject()));
        return ResponseFactory.ok("All sessions revoked");
    }

    @GetMapping("/me")
    @Operation(summary = "Get the profile of the current user")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<UserProfileResponse> me(@AuthenticationPrincipal Jwt jwt) {
        UserProfileResponse profile = authenticationService.currentUserProfile(
                UUID.fromString(jwt.getSubject()));
        return ResponseFactory.ok(profile);
    }
}
