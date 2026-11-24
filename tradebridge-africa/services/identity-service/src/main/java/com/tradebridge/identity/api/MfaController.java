package com.tradebridge.identity.api;

import com.tradebridge.common.response.ApiResponse;
import com.tradebridge.common.response.ResponseFactory;
import com.tradebridge.identity.api.dto.request.MfaSetupRequest;
import com.tradebridge.identity.api.dto.request.MfaVerifyRequest;
import com.tradebridge.identity.api.dto.response.MfaSetupResponse;
import com.tradebridge.identity.api.dto.response.MfaStatusResponse;
import com.tradebridge.identity.service.MfaService;
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

import java.util.List;
import java.util.UUID;

/**
 * MFA endpoints (spec §3.1D).
 *
 * <p>Per spec §11 decision 2: TOTP is the preferred method. Email OTP is a
 * secondary delivery mechanism, not a substitute.
 */
@RestController
@RequestMapping("/api/v1/identity/mfa")
@Tag(name = "MFA", description = "Multi-factor authentication")
@PreAuthorize("isAuthenticated()")
public class MfaController {

    private final MfaService mfaService;

    public MfaController(MfaService mfaService) {
        this.mfaService = mfaService;
    }

    @PostMapping("/setup/totp")
    @Operation(summary = "Begin TOTP setup")
    public ApiResponse<MfaSetupResponse> setupTotp(@AuthenticationPrincipal Jwt jwt,
                                                   @Valid @RequestBody MfaSetupRequest request) {
        UUID userId = UUID.fromString(jwt.getSubject());
        MfaSetupResponse response = mfaService.beginTotpSetup(userId, request);
        return ResponseFactory.ok(response, "Scan the QR code with your authenticator app");
    }

    @PostMapping("/verify-setup")
    @Operation(summary = "Verify TOTP code to confirm setup")
    public ApiResponse<List<String>> verifySetup(@AuthenticationPrincipal Jwt jwt,
                                                 @Valid @RequestBody MfaVerifyRequest request) {
        UUID userId = UUID.fromString(jwt.getSubject());
        List<String> recoveryCodes = mfaService.verifyTotpSetup(userId, request.code());
        return ResponseFactory.ok(recoveryCodes, "MFA enabled — store these recovery codes securely");
    }

    @PostMapping("/disable")
    @Operation(summary = "Disable MFA (requires recent authentication)")
    public ApiResponse<Void> disable(@AuthenticationPrincipal Jwt jwt,
                                     @Valid @RequestBody MfaVerifyRequest request) {
        UUID userId = UUID.fromString(jwt.getSubject());
        mfaService.disable(userId, request.code());
        return ResponseFactory.ok("MFA disabled");
    }

    @PostMapping("/challenge")
    @Operation(summary = "Start an MFA challenge (delivers email OTP)")
    public ApiResponse<Void> challenge(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        mfaService.startEmailChallenge(userId);
        return ResponseFactory.ok("Challenge sent");
    }

    @PostMapping("/verify")
    @Operation(summary = "Verify an MFA challenge")
    public ApiResponse<Void> verify(@AuthenticationPrincipal Jwt jwt,
                                    @Valid @RequestBody MfaVerifyRequest request) {
        UUID userId = UUID.fromString(jwt.getSubject());
        mfaService.verifyChallenge(userId, request.code());
        return ResponseFactory.ok("MFA challenge passed");
    }

    @GetMapping("/status")
    @Operation(summary = "Get MFA status of the current user")
    public ApiResponse<MfaStatusResponse> status(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        MfaStatusResponse response = mfaService.status(userId);
        return ResponseFactory.ok(response);
    }

    @PostMapping("/recovery-codes/regenerate")
    @Operation(summary = "Regenerate recovery codes (requires recent authentication)")
    public ApiResponse<List<String>> regenerate(@AuthenticationPrincipal Jwt jwt,
                                                @Valid @RequestBody MfaVerifyRequest request) {
        UUID userId = UUID.fromString(jwt.getSubject());
        List<String> codes = mfaService.regenerateRecoveryCodes(userId, request.code());
        return ResponseFactory.ok(codes, "Store these codes securely — they will not be shown again");
    }
}
