package com.tradebridge.notification.api;

import com.tradebridge.common.response.ApiResponse;
import com.tradebridge.common.response.ResponseFactory;
import com.tradebridge.notification.api.dto.request.ChannelPreferenceRequest;
import com.tradebridge.notification.api.dto.request.UpdatePreferencesRequest;
import com.tradebridge.notification.api.dto.response.PreferenceResponse;
import com.tradebridge.notification.service.PreferenceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Notification preferences (spec §5.1C).
 *
 * <p>Security-critical messages (e.g. login verification, account alerts)
 * cannot be opted out of — enforced in {@code PreferenceService}.
 */
@RestController
@RequestMapping("/api/v1/notifications/preferences")
@Tag(name = "Preferences", description = "Notification channel preferences")
@PreAuthorize("isAuthenticated()")
public class PreferenceController {

    private final PreferenceService preferenceService;

    public PreferenceController(PreferenceService preferenceService) {
        this.preferenceService = preferenceService;
    }

    @GetMapping
    @Operation(summary = "Get own preferences")
    public ApiResponse<List<PreferenceResponse>> get(@AuthenticationPrincipal Jwt jwt) {
        return ResponseFactory.ok(preferenceService.get(UUID.fromString(jwt.getSubject())));
    }

    @PutMapping
    @Operation(summary = "Update all preferences")
    public ApiResponse<List<PreferenceResponse>> update(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpdatePreferencesRequest request) {
        return ResponseFactory.ok(
                preferenceService.update(UUID.fromString(jwt.getSubject()), request),
                "Preferences updated");
    }

    @PatchMapping("/{channel}")
    @Operation(summary = "Update one channel preference")
    public ApiResponse<PreferenceResponse> updateChannel(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String channel,
            @Valid @RequestBody ChannelPreferenceRequest request) {
        return ResponseFactory.ok(
                preferenceService.updateChannel(UUID.fromString(jwt.getSubject()), channel, request),
                "Preference updated");
    }
}