package com.tradebridge.gateway.security;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * Explicit allowlist of public routes (spec §2.2).
 *
 * <p><b>Design rule: an empty match means protected.</b> Only paths that
 * return {@code true} from {@link #isPublic} bypass authentication.
 *
 * <p>Never add wildcards for whole service prefixes. Each public endpoint
 * is listed explicitly. The current set is derived from spec §2.2 and §3.1:
 * <ul>
 *   <li>Registration, email verification, resend, login, refresh (identity)</li>
 *   <li>Password forgot/reset (identity)</li>
 *   <li>Public country reads (country): GET under /api/v1/countries/**,
 *       /api/v1/currencies, /api/v1/languages</li>
 *   <li>JWKS (identity), actuator health/info, OpenAPI docs</li>
 * </ul>
 *
 * <p>Everything else requires a valid JWT. Downstream services still enforce
 * their own resource-level permissions — the Gateway is only a coarse gate.
 */
@Component
public class PublicRouteRegistry {

    private record Rule(String method, Pattern pattern) {
        boolean matches(String requestMethod, String path) {
            return (method == null || method.equalsIgnoreCase(requestMethod))
                    && pattern.matcher(path).matches();
        }
    }

    private final List<Rule> rules = List.of(
            // ---- Identity: POST public endpoints (spec §2.2, §3.1A/B/C)
            new Rule("POST", Pattern.compile("^/api/v1/identity/register$")),
            new Rule("POST", Pattern.compile("^/api/v1/identity/verify-email$")),
            new Rule("POST", Pattern.compile("^/api/v1/identity/resend-verification$")),
            new Rule("POST", Pattern.compile("^/api/v1/identity/login$")),
            new Rule("POST", Pattern.compile("^/api/v1/identity/refresh$")),
            new Rule("POST", Pattern.compile("^/api/v1/identity/password/forgot$")),
            new Rule("POST", Pattern.compile("^/api/v1/identity/password/reset$")),

            // ---- Country: all public GETs (spec §2.2, §4.1A)
            new Rule("GET", Pattern.compile("^/api/v1/countries(/.*)?$")),
            new Rule("GET", Pattern.compile("^/api/v1/currencies$")),
            new Rule("GET", Pattern.compile("^/api/v1/languages$")),

            // ---- Discovery / ops
            new Rule("GET", Pattern.compile("^/\\.well-known/jwks\\.json$")),
            new Rule("GET", Pattern.compile("^/actuator/health(/.*)?$")),
            new Rule("GET", Pattern.compile("^/actuator/info$")),
            new Rule("GET", Pattern.compile("^/v3/api-docs(/.*)?$")),
            new Rule("GET", Pattern.compile("^/swagger-ui(/.*)?$"))
    );

    /**
     * True if the request bypasses authentication. Default = false.
     */
    public boolean isPublic(String method, String path) {
        if (path == null) return false;
        String normalized = path.split("\\?")[0];
        for (Rule r : rules) {
            if (r.matches(method, normalized)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Read-only view for testing / diagnostics.
     */
    public List<String> describeRules() {
        return rules.stream()
                .map(r -> r.method() + " " + r.pattern().pattern())
                .toList();
    }
}