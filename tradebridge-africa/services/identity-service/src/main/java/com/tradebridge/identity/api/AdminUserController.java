package com.tradebridge.identity.api;

import com.tradebridge.common.response.ApiResponse;
import com.tradebridge.common.response.PageResponse;
import com.tradebridge.common.response.ResponseFactory;
import com.tradebridge.common.pagination.PageRequest;
import com.tradebridge.identity.api.dto.request.AssignRolesRequest;
import com.tradebridge.identity.api.dto.request.UpdateUserStatusRequest;
import com.tradebridge.identity.api.dto.response.PermissionResponse;
import com.tradebridge.identity.api.dto.response.RoleResponse;
import com.tradebridge.identity.api.dto.response.UserProfileResponse;
import com.tradebridge.identity.service.PermissionService;
import com.tradebridge.identity.service.RoleService;
import com.tradebridge.identity.service.UserAdminService;
import com.tradebridge.security.annotation.RequirePermission;
import com.tradebridge.security.authorization.PermissionCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * User administration endpoints (spec §3.1E).
 */
@RestController
@RequestMapping("/api/v1/identity/admin")
@Tag(name = "Admin", description = "User, role, and permission administration")
public class AdminUserController {

    private final UserAdminService userAdminService;
    private final RoleService roleService;
    private final PermissionService permissionService;

    public AdminUserController(UserAdminService userAdminService,
                               RoleService roleService,
                               PermissionService permissionService) {
        this.userAdminService = userAdminService;
        this.roleService = roleService;
        this.permissionService = permissionService;
    }

    @GetMapping("/users")
    @Operation(summary = "Search users")
    @RequirePermission(PermissionCode.IDENTITY_USER_READ)
    public ApiResponse<PageResponse<UserProfileResponse>> searchUsers(
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageResponse<UserProfileResponse> result =
                userAdminService.search(query, PageRequest.of(page, size));
        return ResponseFactory.ok(result.getContent().isEmpty()
                ? PageResponse.empty(result.getPage())
                : result);
    }

    @GetMapping("/users/{userId}")
    @Operation(summary = "View user")
    @RequirePermission(PermissionCode.IDENTITY_USER_READ)
    public ApiResponse<UserProfileResponse> getUser(@PathVariable UUID userId) {
        return ResponseFactory.ok(userAdminService.getUser(userId));
    }

    @PatchMapping("/users/{userId}/status")
    @Operation(summary = "Suspend or reactivate a user")
    @RequirePermission(PermissionCode.IDENTITY_USER_ADMIN)
    public ApiResponse<Void> updateStatus(@PathVariable UUID userId,
                                          @Valid @RequestBody UpdateUserStatusRequest request,
                                          @AuthenticationPrincipal Jwt jwt) {
        UUID actor = UUID.fromString(jwt.getSubject());
        userAdminService.updateStatus(userId, request, actor);
        return ResponseFactory.ok("User status updated");
    }

    @PostMapping("/users/{userId}/unlock")
    @Operation(summary = "Unlock a locked account")
    @RequirePermission(PermissionCode.IDENTITY_USER_ADMIN)
    public ApiResponse<Void> unlock(@PathVariable UUID userId,
                                    @AuthenticationPrincipal Jwt jwt) {
        UUID actor = UUID.fromString(jwt.getSubject());
        userAdminService.unlock(userId, actor);
        return ResponseFactory.ok("User unlocked");
    }

    @GetMapping("/roles")
    @Operation(summary = "List roles")
    @RequirePermission(PermissionCode.IDENTITY_ROLE_READ)
    public ApiResponse<List<RoleResponse>> listRoles() {
        return ResponseFactory.ok(roleService.listAll());
    }

    @GetMapping("/permissions")
    @Operation(summary = "List permissions")
    @RequirePermission(PermissionCode.IDENTITY_PERMISSION_READ)
    public ApiResponse<List<PermissionResponse>> listPermissions() {
        return ResponseFactory.ok(permissionService.listAll());
    }

    @PutMapping("/users/{userId}/roles")
    @Operation(summary = "Assign roles to a user")
    @RequirePermission(PermissionCode.IDENTITY_ROLE_WRITE)
    public ApiResponse<Void> assignRoles(@PathVariable UUID userId,
                                         @Valid @RequestBody AssignRolesRequest request,
                                         @AuthenticationPrincipal Jwt jwt) {
        UUID actor = UUID.fromString(jwt.getSubject());
        roleService.assignRoles(userId, request.roleCodes(), actor);
        return ResponseFactory.ok("Roles assigned");
    }
}
