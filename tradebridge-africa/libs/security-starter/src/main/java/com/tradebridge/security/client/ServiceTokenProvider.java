package com.tradebridge.security.client;

import io.jsonwebtoken.Jwts;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.InputStream;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;

/**
 * Issues short-lived service JWTs for outbound service-to-service calls.
 *
 * <p>Downstream services verify these with the same machinery as user tokens:
 * claim {@code ttyp=service} marks the token as a service account, and
 * {@code aud} is set to the well-known service audience, not the user audience.
 * The Gateway MUST reject service tokens on user-facing routes.
 *
 * <p>Tokens are cached for reuse until shortly before expiry to avoid
 * re-signing on every outbound call.
 */
public class ServiceTokenProvider {

    private static final long REFRESH_SKEW_SECONDS = 30;

    private final ServiceIdentityProperties properties;
    private final ResourceLoader resourceLoader;

    private volatile PrivateKey privateKey;
    private volatile String cachedToken;
    private volatile Instant cachedTokenExpiresAt = Instant.EPOCH;

    public ServiceTokenProvider(ServiceIdentityProperties properties,
                                ResourceLoader resourceLoader) {
        this.properties = Objects.requireNonNull(properties, "properties");
        this.resourceLoader = Objects.requireNonNull(resourceLoader, "resourceLoader");
        if (properties.getPrivateKeyLocation() == null) {
            throw new IllegalStateException(
                    "tradebridge.security.service-identity.private-key-location must be set to issue service tokens");
        }
    }

    /** Returns a signed service JWT valid for the configured TTL. */
    public String getToken() {
        Instant now = Instant.now();
        if (cachedToken != null && cachedTokenExpiresAt.minusSeconds(REFRESH_SKEW_SECONDS).isAfter(now)) {
            return cachedToken;
        }
        synchronized (this) {
            if (cachedToken != null && cachedTokenExpiresAt.minusSeconds(REFRESH_SKEW_SECONDS).isAfter(now)) {
                return cachedToken;
            }
            Instant expiresAt = now.plus(properties.getTtl());
            String token = buildToken(now, expiresAt);
            this.cachedTokenExpiresAt = expiresAt;
            this.cachedToken = token;
            return token;
        }
    }

    /** Authorization header value ({@code "Bearer ..."}) for outbound calls. */
    public String getAuthorizationHeader() {
        return "Bearer " + getToken();
    }

    private String buildToken(Instant issuedAt, Instant expiresAt) {
        return Jwts.builder()
                .header().keyId(properties.getKeyId()).and()
                .issuer(properties.getIssuer())
                .audience().add(properties.getAudience()).and()
                .subject(properties.getCallerServiceId() != null
                        ? properties.getCallerServiceId()
                        : "anonymous-service")
                .id(UUID.randomUUID().toString())
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .claim("ttyp", "service")
                .claim("scp", java.util.List.of(properties.getAudience()))
                .signWith(loadPrivateKey(), Jwts.SIG.RS256)
                .compact();
    }

    private PrivateKey loadPrivateKey() {
        if (privateKey != null) {
            return privateKey;
        }
        synchronized (this) {
            if (privateKey != null) {
                return privateKey;
            }
            try {
                Resource resource = resourceLoader.getResource(properties.getPrivateKeyLocation());
                try (InputStream in = resource.getInputStream()) {
                    String pem = new String(in.readAllBytes())
                            .replace("-----BEGIN PRIVATE KEY-----", "")
                            .replace("-----END PRIVATE KEY-----", "")
                            .replaceAll("\\s", "");
                    byte[] der = Base64.getDecoder().decode(pem);
                    PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(der);
                    this.privateKey = KeyFactory.getInstance("RSA").generatePrivate(spec);
                    return privateKey;
                }
            } catch (Exception e) {
                throw new IllegalStateException("Failed to load service private key", e);
            }
        }
    }
}
