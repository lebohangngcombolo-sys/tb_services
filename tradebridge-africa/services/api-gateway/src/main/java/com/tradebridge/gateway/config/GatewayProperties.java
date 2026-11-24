package com.tradebridge.gateway.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Gateway configuration (spec §2.3).
 *
 * <p>Structure mirrors {@code tradebridge.gateway.*} in
 * {@code application.yml}. Route definitions are declarative; the
 * {@link RouteConfig} translates them to Spring Cloud Gateway routes at
 * startup.
 */
@Validated
@ConfigurationProperties(prefix = "tradebridge.gateway")
public class GatewayProperties {

    private final Jwt jwt = new Jwt();
    private final Cors cors = new Cors();
    private final Timeout timeout = new Timeout();
    private final Tracing tracing = new Tracing();
    private final RateLimit rateLimit = new RateLimit();
    private final List<RouteDefinition> routes = new ArrayList<>();

    public Jwt getJwt() { return jwt; }
    public Cors getCors() { return cors; }
    public Timeout getTimeout() { return timeout; }
    public Tracing getTracing() { return tracing; }
    public RateLimit getRateLimit() { return rateLimit; }
    public List<RouteDefinition> getRoutes() { return routes; }

    // ------------------------------------------------------------------ JWT

    public static class Jwt {
        @NotBlank private String issuer;
        @NotBlank private String audience;
        @NotBlank private String jwksUri;
        private String publicKeyLocation;
        private String keyId;
        private Duration clockSkew = Duration.ofSeconds(30);

        public String getIssuer() { return issuer; }
        public void setIssuer(String v) { this.issuer = v; }
        public String getAudience() { return audience; }
        public void setAudience(String v) { this.audience = v; }
        public String getJwksUri() { return jwksUri; }
        public void setJwksUri(String v) { this.jwksUri = v; }
        public String getPublicKeyLocation() { return publicKeyLocation; }
        public void setPublicKeyLocation(String v) { this.publicKeyLocation = v; }
        public String getKeyId() { return keyId; }
        public void setKeyId(String v) { this.keyId = v; }
        public Duration getClockSkew() { return clockSkew; }
        public void setClockSkew(Duration v) { this.clockSkew = v; }
    }

    // ------------------------------------------------------------------ CORS

    public static class Cors {
        private List<String> allowedOrigins = new ArrayList<>();
        private List<String> allowedMethods = List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");
        private List<String> allowedHeaders = List.of("*");
        private List<String> exposedHeaders = List.of("X-Correlation-Id");
        private boolean allowCredentials = false;
        private Duration maxAge = Duration.ofHours(1);

        public List<String> getAllowedOrigins() { return allowedOrigins; }
        public void setAllowedOrigins(List<String> v) { this.allowedOrigins = v; }
        public List<String> getAllowedMethods() { return allowedMethods; }
        public void setAllowedMethods(List<String> v) { this.allowedMethods = v; }
        public List<String> getAllowedHeaders() { return allowedHeaders; }
        public void setAllowedHeaders(List<String> v) { this.allowedHeaders = v; }
        public List<String> getExposedHeaders() { return exposedHeaders; }
        public void setExposedHeaders(List<String> v) { this.exposedHeaders = v; }
        public boolean isAllowCredentials() { return allowCredentials; }
        public void setAllowCredentials(boolean v) { this.allowCredentials = v; }
        public Duration getMaxAge() { return maxAge; }
        public void setMaxAge(Duration v) { this.maxAge = v; }
    }

    // ------------------------------------------------------------------ Timeout

    public static class Timeout {
        private Duration connect = Duration.ofSeconds(5);
        private Duration response = Duration.ofSeconds(30);

        public Duration getConnect() { return connect; }
        public void setConnect(Duration v) { this.connect = v; }
        public Duration getResponse() { return response; }
        public void setResponse(Duration v) { this.response = v; }
    }

    // ------------------------------------------------------------------ Tracing

    public static class Tracing {
        private boolean propagateCorrelationId = true;
        private String correlationHeader = "X-Correlation-Id";

        public boolean isPropagateCorrelationId() { return propagateCorrelationId; }
        public void setPropagateCorrelationId(boolean v) { this.propagateCorrelationId = v; }
        public String getCorrelationHeader() { return correlationHeader; }
        public void setCorrelationHeader(String v) { this.correlationHeader = v; }
    }

    // ------------------------------------------------------------------ Rate limits

    public static class RateLimit {
        private final EndpointLimit login = new EndpointLimit(10, Duration.ofMinutes(1));
        private final EndpointLimit otp = new EndpointLimit(5, Duration.ofMinutes(1));
        private final EndpointLimit passwordReset = new EndpointLimit(3, Duration.ofMinutes(1));
        private final EndpointLimit registration = new EndpointLimit(5, Duration.ofMinutes(1));

        public EndpointLimit getLogin() { return login; }
        public EndpointLimit getOtp() { return otp; }
        public EndpointLimit getPasswordReset() { return passwordReset; }
        public EndpointLimit getRegistration() { return registration; }
    }

    public static class EndpointLimit {
        @Positive private int requestsPerWindow;
        private Duration window;

        public EndpointLimit() { }

        public EndpointLimit(int requestsPerWindow, Duration window) {
            this.requestsPerWindow = requestsPerWindow;
            this.window = window;
        }

        public int getRequestsPerWindow() { return requestsPerWindow; }
        public void setRequestsPerWindow(int v) { this.requestsPerWindow = v; }
        public Duration getWindow() { return window; }
        public void setWindow(Duration v) { this.window = v; }
    }

    // ------------------------------------------------------------------ Routes

    public static class RouteDefinition {
        @NotBlank private String id;
        private String path;
        @NotBlank private String uri;
        private String stripPrefix;
        private Map<String, String> metadata = Map.of();

        public String getId() { return id; }
        public void setId(String v) { this.id = v; }
        public String getPath() { return path; }
        public void setPath(String v) { this.path = v; }
        public String getUri() { return uri; }
        public void setUri(String v) { this.uri = v; }
        public String getStripPrefix() { return stripPrefix; }
        public void setStripPrefix(String v) { this.stripPrefix = v; }
        public Map<String, String> getMetadata() { return metadata; }
        public void setMetadata(Map<String, String> v) { this.metadata = v; }
    }
}