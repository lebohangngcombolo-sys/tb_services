package com.tradebridge.notification.api;

import com.tradebridge.common.response.ApiResponse;
import com.tradebridge.common.response.PageResponse;
import com.tradebridge.common.response.ResponseFactory;
import com.tradebridge.notification.api.dto.response.NotificationResponse;
import com.tradebridge.notification.api.dto.response.UnreadCountResponse;
import com.tradebridge.notification.service.NotificationCenterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Customer notification centre (spec §5.1B). All endpoints act on the
 * current authenticated principal's notifications only.
 */
@RestController
@RequestMapping("/api/v1/notifications/me")
@Tag(name = "My Notifications", description = "Customer notification centre")
@PreAuthorize("isAuthenticated()")
public class MyNotificationsController {

    private final NotificationCenterService centerService;

    public MyNotificationsController(NotificationCenterService centerService) {
        this.centerService = centerService;
    }

    @GetMapping
    @Operation(summary = "List own notifications")
    public ApiResponse<PageResponse<NotificationResponse>> list(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Boolean unreadOnly) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return ResponseFactory.ok(centerService.list(userId, page, size, unreadOnly));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Count unread notifications")
    public ApiResponse<UnreadCountResponse> unreadCount(@AuthenticationPrincipal Jwt jwt) {
        return ResponseFactory.ok(centerService.unreadCount(UUID.fromString(jwt.getSubject())));
    }

    @GetMapping("/{notificationId}")
    @Operation(summary = "Get own notification")
    public ApiResponse<NotificationResponse> get(@AuthenticationPrincipal Jwt jwt,
                                                  @PathVariable UUID notificationId) {
        return ResponseFactory.ok(centerService.get(UUID.fromString(jwt.getSubject()), notificationId));
    }

    @PatchMapping("/{notificationId}/read")
    @Operation(summary = "Mark own notification as read")
    public ApiResponse<Void> markRead(@AuthenticationPrincipal Jwt jwt,
                                       @PathVariable UUID notificationId) {
        centerService.markRead(UUID.fromString(jwt.getSubject()), notificationId);
        return ResponseFactory.ok("Notification marked as read");
    }

    @PatchMapping("/read-all")
    @Operation(summary = "Mark all own notifications as read")
    public ApiResponse<Void> markAllRead(@AuthenticationPrincipal Jwt jwt) {
        centerService.markAllRead(UUID.fromString(jwt.getSubject()));
        return ResponseFactory.ok("All notifications marked as read");
    }

    @DeleteMapping("/{notificationId}")
    @Operation(summary = "Archive own notification")
    public ApiResponse<Void> archive(@AuthenticationPrincipal Jwt jwt,
                                      @PathVariable UUID notificationId) {
        centerService.archive(UUID.fromString(jwt.getSubject()), notificationId);
        return ResponseFactory.ok("Notification archived");
    }
}