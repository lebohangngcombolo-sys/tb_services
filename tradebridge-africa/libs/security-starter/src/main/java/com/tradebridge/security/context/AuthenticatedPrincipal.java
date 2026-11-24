package com.tradebridge.security.context;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Typed view of the current principal, extracted from the authenticated
 * {@link org.springframework.security.core.Authentication}.
 *
 * <p>Deliberately excludes email, phone, and all personal data — those
 * must be fetched from the owning service. Only identifiers, roles and
 * permissions live on the token (§3.4: "Never put passwords, OTPs or
 * sensitive personal data in JWT claims").
 */
public final class AuthenticatedPrincipal {

    private final UUID userId;
    private final UUID sessionId;
    private final Set<String> roles;
    private final Set<String> permissions;
    private final boolean serviceAccount;
    private final String subject;

    public AuthenticatedPrincipal(UUID userId,
                                  UUID sessionId,
                                  Set<String> roles,
                                  Set<String> permissions,
                                  boolean serviceAccount,
                                  String subject) {
        this.userId = userId;
        this.sessionId = sessionId;
        this.roles = roles == null ? Collections.emptySet() : Set.copyOf(roles);
        this.permissions = permissions == null ? Collections.emptySet() : Set.copyOf(permissions);
        this.serviceAccount = serviceAccount;
        this.subject = Objects.requireNonNull(subject, "subject");
    }

    public UUID getUserId() { return userId; }
    public UUID getSessionId() { return sessionId; }
    public Set<String> getRoles() { return roles; }
    public Set<String> getPermissions() { return permissions; }
    public boolean isServiceAccount() { return serviceAccount; }
    public String getSubject() { return subject; }

    public boolean hasRole(String role) { return roles.contains(role); }
    public boolean hasPermission(String permission) { return permissions.contains(permission); }
}
