package com.tradebridge.security.client;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Service-to-service identity configuration. Service JWTs are short-lived
 * (default 5m) and marked with {@code ttyp=service} so that user-facing
 * filters can distinguish them from user tokens.
 *
 * <pre>
 * tradebridge.security.service-identity:
 *   issuer: tradebridge-internal
 *   audience: tradebridge-services
 *   private-key-location: classpath:keys/service-private.pem
 *   key-id: svc-2026-01
 *   ttl: 5m
 * </pre>
 */
@ConfigurationProperties(prefix = "tradebridge.security.service-identity")
public class ServiceIdentityProperties {

    private String issuer = "tradebridge-internal";
    private String audience = "tradebridge-services";
    private String privateKeyLocation;
    private String keyId;
    private Duration ttl = Duration.ofMinutes(5);
    private String callerServiceId;

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
    public String getCallerServiceId() { return callerServiceId; }
    public void setCallerServiceId(String v) { this.callerServiceId = v; }
}
