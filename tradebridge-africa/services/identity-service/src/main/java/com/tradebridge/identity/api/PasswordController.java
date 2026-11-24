package com.tradebridge.identity.api;

import com.tradebridge.common.response.ApiResponse;
import com.tradebridge.common.response.ResponseFactory;
import com.tradebridge.identity.api.dto.request.ChangePasswordRequest;
import com.tradebridge.identity.api.dto.request.ForgotPasswordRequest;
import com.tradebridge.identity.api.dto.request.ResetPasswordRequest;
import com.tradebridge.identity.service.PasswordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

/**
 * Password management endpoints (spec §3.1C).
 *
 * <p>/forgot and /reset are public. /change and /validate require a JWT.
 */
@RestController
@RequestMapping("/api/v1/identity/password")
@Tag(name = "Password", description = "Password reset and change")
public class PasswordController {

    private final PasswordService passwordService;

    public PasswordController(PasswordService passwordService) {
        this.passwordService = passwordService;
    }

    @PostMapping("/forgot")
    @Operation(summary = "Request a password reset (public)")
    public ApiResponse<Void> forgot(@Valid @RequestBody ForgotPasswordRequest request) {
        passwordService.requestReset(request);
        return ResponseFactory.ok("If the account exists, a reset email has been sent");
    }

    @PostMapping("/reset")
    @Operation(summary = "Reset password using token (public)")
    public ApiResponse<Void> reset(@Valid @RequestBody ResetPasswordRequest request) {
        passwordService.reset(request);
        return ResponseFactory.ok("Password reset");
    }

    @PostMapping("/change")
    @Operation(summary = "Change password for the current user")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> change(@AuthenticationPrincipal Jwt jwt,
                                    @Valid @RequestBody ChangePasswordRequest request) {
        UUID userId = UUID.fromString(jwt.getSubject());
        UUID sessionId = UUID.fromString(jwt.getClaimAsString("sid"));
        passwordService.change(userId, sessionId, request);
        return ResponseFactory.ok("Password changed");
    }

    @PostMapping("/validate")
    @Operation(summary = "Validate the current password")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Map<String, Boolean>> validate(@AuthenticationPrincipal Jwt jwt,
                                                      @RequestBody Map<String, String> body) {
        UUID userId = UUID.fromString(jwt.getSubject());
        boolean ok = passwordService.validateCurrentPassword(userId, body.get("password"));
        return ResponseFactory.ok(Map.of("valid", ok));
    }
}
