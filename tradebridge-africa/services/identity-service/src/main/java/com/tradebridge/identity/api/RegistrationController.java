package com.tradebridge.identity.api;

import com.tradebridge.common.response.ApiResponse;
import com.tradebridge.common.response.ResponseFactory;
import com.tradebridge.identity.api.dto.request.RegisterRequest;
import com.tradebridge.identity.api.dto.request.VerifyEmailRequest;
import com.tradebridge.identity.api.dto.response.RegistrationResponse;
import com.tradebridge.identity.service.EmailVerificationService;
import com.tradebridge.identity.service.RegistrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

/**
 * Registration and account activation endpoints (spec §3.1A).
 *
 * <p>Public endpoints (per spec §2.2): /register, /verify-email, /resend-verification.
 * /verification-status is authenticated. /activate is internal-only.
 */
@RestController
@RequestMapping("/api/v1/identity")
@Tag(name = "Registration", description = "User registration and account activation")
public class RegistrationController {

    private final RegistrationService registrationService;
    private final EmailVerificationService verificationService;

    public RegistrationController(RegistrationService registrationService,
                                  EmailVerificationService verificationService) {
        this.registrationService = registrationService;
        this.verificationService = verificationService;
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new user (public)")
    public ResponseEntity<ApiResponse<RegistrationResponse>> register(
            @Valid @RequestBody RegisterRequest request) {
        RegistrationResponse response = registrationService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResponseFactory.ok(response, "Registration accepted"));
    }

    @PostMapping("/verify-email")
    @Operation(summary = "Verify email with OTP (public)")
    public ApiResponse<Void> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        verificationService.verifyEmail(request);
        return ResponseFactory.ok("Email verified");
    }

    @PostMapping("/resend-verification")
    @Operation(summary = "Resend verification OTP (public)")
    public ApiResponse<Void> resendVerification(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        verificationService.resendVerification(email);
        return ResponseFactory.ok("Verification email re-sent if the account exists");
    }

    @GetMapping("/verification-status")
    @Operation(summary = "Get verification status of the current user")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Map<String, Object>> verificationStatus() {
        return ResponseFactory.ok(verificationService.currentUserStatus());
    }

    /**
     * Internal endpoint used after required checks complete (e.g. an admin
     * approval flow). Not exposed publicly by the Gateway (spec §2.2).
     */
    @PostMapping("/activate")
    @Operation(summary = "Activate an account (internal)")
    public ApiResponse<Void> activate(@RequestBody Map<String, String> body) {
        UUID userId = UUID.fromString(body.get("userId"));
        registrationService.activate(userId);
        return ResponseFactory.ok("Account activated");
    }
}
