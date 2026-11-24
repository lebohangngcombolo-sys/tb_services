package com.tradebridge.security.authorization;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Resolves whether an authenticated principal possesses a required permission
 * or role, respecting the hierarchy configured at
 * {@code tradebridge.security.permissions.hierarchy}.
 *
 * <p>Authorities are expected to be plain strings (e.g. {@code "KYC_ANALYST"}
 * for roles, {@code "kyc.case.review"} for permissions). Prefix conventions
 * are not enforced — the caller decides what a value means.
 */
public class PermissionEvaluator {

    private final RoleHierarchy roleHierarchy;

    public PermissionEvaluator(RoleHierarchy roleHierarchy) {
        this.roleHierarchy = Objects.requireNonNull(roleHierarchy, "roleHierarchy");
    }

    public boolean hasRole(Authentication authentication, String role) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        Set<String> expanded = expand(authentication.getAuthorities());
        return expanded.contains(role);
    }

    public boolean hasAnyRole(Authentication authentication, Collection<String> roles) {
        if (roles == null || roles.isEmpty()) {
            return true;
        }
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        Set<String> expanded = expand(authentication.getAuthorities());
        return roles.stream().anyMatch(expanded::contains);
    }

    public boolean hasPermission(Authentication authentication, String permission) {
        return hasRole(authentication, permission);
    }

    public boolean hasAnyPermission(Authentication authentication, Collection<String> permissions) {
        return hasAnyRole(authentication, permissions);
    }

    public boolean hasAllPermissions(Authentication authentication, Collection<String> permissions) {
        if (permissions == null || permissions.isEmpty()) {
            return true;
        }
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        Set<String> expanded = expand(authentication.getAuthorities());
        return permissions.stream().allMatch(expanded::contains);
    }

    private Set<String> expand(Collection<? extends GrantedAuthority> authorities) {
        Set<String> raw = authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        return roleHierarchy.expand(raw);
    }
}
