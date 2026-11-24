package com.tradebridge.security.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

/**
 * Central configuration for the security starter.
 *
 * <pre>
 * tradebridge.security:
 *   jwt:
 *     issuer: "https://identity.tradebridge.africa"
 *     audience: "tradebridge-clients"
 *     jwks-uri: "http://identity-service:8081/.well-known/jwks.json"
 *     public-key-location: "classpath:keys/jwt-public.pem"     # optional alternative to JWKS
 *     private-key-location: "classpath:keys/jwt-private.pem"  # only on identity-service
 *     key-id: "tb-key-2026-01"
 *     clock-skew: 30s
 *     refresh-jwks-before-expiry: 5m
 *   service-identity:
 *     issuer: "tradebridge-internal"
 *     audience: "tradebridge-services"
 *     private-key-location: "classpath:keys/service-private.pem"
 *     key-id: "svc-2026-01"
 *     ttl: 5m
 *   permissions:
 *     hierarchy:
 *       SUPER_ADMIN: ["ADMIN"]
 *       ADMIN: ["KYC_ANALYST", "COMPLIANCE_OFFICER", "SUPPORT"]
 * </pre>
 */
@ConfigurationProperties(prefix = "tradebridge.security")
public class SecurityProperties {

    private final Jwt jwt = new Jwt();
    private final ServiceIdentity serviceIdentity = new ServiceIdentity();
    private final Permissions permissions = new Permissions();
    private final Audit audit = new Audit();

    public Jwt getJwt() { return jwt; }
    public ServiceIdentity getServiceIdentity() { return serviceIdentity; }
    public Permissions getPermissions() { return permissions; }
    public Audit getAudit() { return audit; }

    public static class Jwt {
        private String issuer;
        private String audience;
        private String jwksUri;
        private String publicKeyLocation;
        private String privateKeyLocation;
        private String keyId;
        private Duration clockSkew = Duration.ofSeconds(30);
        private Duration refreshJwksBeforeExpiry = Duration.ofMinutes(5);
        private String signingAlgorithm = "RS256";

        public String getIssuer() { return issuer; }
        public void setIssuer(String issuer) { this.issuer = issuer; }
        public String getAudience() { return audience; }
        public void setAudience(String audience) { this.audience = audience; }
        public String getJwksUri() { return jwksUri; }
        public void setJwksUri(String jwksUri) { this.jwksUri = jwksUri; }
        public String getPublicKeyLocation() { return publicKeyLocation; }
        public void setPublicKeyLocation(String v) { this.publicKeyLocation = v; }
        public String getPrivateKeyLocation() { return privateKeyLocation; }
        public void setPrivateKeyLocation(String v) { this.privateKeyLocation = v; }
        public String getKeyId() { return keyId; }
        public void setKeyId(String keyId) { this.keyId = keyId; }
        public Duration getClockSkew() { return clockSkew; }
        public void setClockSkew(Duration d) { this.clockSkew = d; }
        public Duration getRefreshJwksBeforeExpiry() { return refreshJwksBeforeExpiry; }
        public void setRefreshJwksBeforeExpiry(Duration d) { this.refreshJwksBeforeExpiry = d; }
        public String getSigningAlgorithm() { return signingAlgorithm; }
        public void setSigningAlgorithm(String a) { this.signingAlgorithm = a; }
    }

    public static class ServiceIdentity {
        private String issuer = "tradebridge-internal";
        private String audience = "tradebridge-services";
        private String privateKeyLocation;
        private String keyId;
        private Duration ttl = Duration.ofMinutes(5);

        public String getIssuer() { return issuer; }
        public void setIssuer(String v) { this.issuer = v; }
        public String getAudience() { return audience; }
        public void setAudience(String v) { this.audience = v; }
        public String getPrivateKeyLocation() { return privateKeyLocation; }
        public void setPrivateKeyLocation(String v) { this.privateKeyLocation = v; }
        public String getKeyId() { return keyId; }
        public void setKeyId(String v) { this.keyId = v; }
        public Duration getTtl() { return ttl; }
        public void setTtl(Duration d) { this.ttl = d; }
    }

    public static class Permissions {
        private java.util.Map<String, List<String>> hierarchy = java.util.Map.of();

        public java.util.Map<String, List<String>> getHierarchy() { return hierarchy; }
        public void setHierarchy(java.util.Map<String, List<String>> h) { this.hierarchy = h; }
    }

    public static class Audit {
        private boolean enabled = true;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }
}
