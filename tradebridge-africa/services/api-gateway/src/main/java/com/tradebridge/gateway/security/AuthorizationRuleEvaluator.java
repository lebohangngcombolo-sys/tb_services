package com.tradebridge.gateway.security;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Coarse-grained authorization rules applied at the edge (spec §2.2).
 *
 * <p>The spec is explicit: "Downstream services must also enforce
 * authorization. Never rely on the Gateway alone for resource ownership
 * or business-level permissions." So this evaluator only checks:
 * <ul>
 *   <li>Which role/permission category a route requires.</li>
 *   <li>Whether the JWT's roles claim satisfies it (through the standard
 *       configured hierarchy).</li>
 * </ul>
 * It does NOT check resource ownership (that's the downstream service's job).
 *
 * <p>Rules:
 * <ul>
 *   <li>KYC routes require {@code KYC_ANALYST} or higher.</li>
 *   <li>Compliance routes require {@code COMPLIANCE_OFFICER} or higher.</li>
 *   <li>Admin routes require {@code ADMIN} or higher.</li>
 *   <li>Super-admin routes require {@code SUPER_ADMIN}.</li>
 * </ul>
 */
@Component
public class AuthorizationRuleEvaluator {

    /** Role hierarchy: a SUPER_ADMIN implies ADMIN implies everything else. */
    private static final java.util.Map<String, List<String>> HIERARCHY = java.util.Map.of(
            "SUPER_ADMIN", List.of("ADMIN"),
            "ADMIN", List.of("KYC_ANALYST", "KYC_SUPERVISOR", "COMPLIANCE_OFFICER", "SUPPORT")
    );

    /** Route prefix → minimum role required. */
    private record Rule(String prefix, String role) { }
    private static final List<Rule> RULES = List.of(
            new Rule("/api/v1/identity/admin/roles", "SUPER_ADMIN"),
            new Rule("/api/v1/identity/admin/users", "ADMIN"),
            new Rule("/api/v1/identity/admin/permissions", "ADMIN"),
            new Rule("/api/v1/kyc/", "KYC_ANALYST"),
            new Rule("/api/v1/customers/admin", "ADMIN"),
            new Rule("/api/v1/countries/admin", "SUPER_ADMIN")
    );

    /**
     * Returns true if the JWT satisfies the coarse rule for the request path,
     * or if no rule applies (downstream decides).
     */
    public boolean isAllowed(Jwt jwt, String path) {
        if (jwt == null || path == null) return true;
        List<String> rules = findRules(path);
        if (rules.isEmpty()) return true;
        List<String> jwtRoles = jwt.getClaimAsStringList("roles");
        if (jwtRoles == null) jwtRoles = List.of();
        for (String required : rules) {
            if (hasRoleOrImplied(jwtRoles, required)) {
                return true;
            }
        }
        return false;
    }

    private static List<String> findRules(String path) {
        return RULES.stream()
                .filter(r -> path.startsWith(r.prefix))
                .map(Rule::role)
                .toList();
    }

    private static boolean hasRoleOrImplied(List<String> roles, String required) {
        if (roles.contains(required)) return true;
        for (String role : roles) {
            if (implies(role, required, new java.util.HashSet<>())) {
                return true;
            }
        }
        return false;
    }

    private static boolean implies(String role, String required, java.util.Set<String> visited) {
        if (!visited.add(role)) return false;
        List<String> implied = HIERARCHY.getOrDefault(role, List.of());
        if (implied.contains(required)) return true;
        for (String next : implied) {
            if (implies(next, required, visited)) return true;
        }
        return false;
    }
}