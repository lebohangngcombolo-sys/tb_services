package com.tradebridge.security.authorization;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Static role → implied-role graph (transitive). Example:
 * {@code SUPER_ADMIN -> ADMIN -> KYC_ANALYST, COMPLIANCE_OFFICER, SUPPORT}.
 *
 * <p>Populated from {@code tradebridge.security.permissions.hierarchy}.
 * Immutable after construction. Transitive closure computed once so the
 * {@link PermissionEvaluator} is O(1) per check.
 */
public class RoleHierarchy {

    private final Map<String, Set<String>> impliedByRole;

    public RoleHierarchy(Map<String, ? extends Iterable<String>> rawHierarchy) {
        Map<String, Set<String>> resolved = new HashMap<>();
        if (rawHierarchy != null) {
            for (Map.Entry<String, ? extends Iterable<String>> e : rawHierarchy.entrySet()) {
                Set<String> direct = new HashSet<>();
                e.getValue().forEach(direct::add);
                resolved.put(e.getKey(), direct);
            }
        }
        this.impliedByRole = deepClosure(resolved);
    }

    /**
     * Expands the given raw authorities to include all implied roles.
     */
    public Set<String> expand(Set<String> raw) {
        Set<String> result = new HashSet<>(raw);
        for (String authority : raw) {
            Set<String> implied = impliedByRole.get(authority);
            if (implied != null) {
                result.addAll(implied);
            }
        }
        return result;
    }

    public Set<String> impliedRoles(String role) {
        return impliedByRole.getOrDefault(role, Collections.emptySet());
    }

    private static Map<String, Set<String>> deepClosure(Map<String, Set<String>> direct) {
        Map<String, Set<String>> result = new HashMap<>();
        for (String key : direct.keySet()) {
            Set<String> closure = new HashSet<>();
            collectTransitive(key, direct, closure, new HashSet<>());
            result.put(key, Collections.unmodifiableSet(closure));
        }
        return Collections.unmodifiableMap(result);
    }

    private static void collectTransitive(String role,
                                          Map<String, Set<String>> direct,
                                          Set<String> accumulator,
                                          Set<String> visited) {
        if (!visited.add(role)) {
            return;
        }
        Set<String> implied = direct.get(role);
        if (implied == null) {
            return;
        }
        for (String next : implied) {
            if (accumulator.add(next)) {
                collectTransitive(next, direct, accumulator, visited);
            }
        }
    }
}
