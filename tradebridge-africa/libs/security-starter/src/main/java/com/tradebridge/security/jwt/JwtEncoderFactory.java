package com.tradebridge.security.jwt;

import com.tradebridge.security.config.SecurityProperties;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtClaimNames;

import java.io.InputStream;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Objects;

/**
 * Asymmetric JWT signing used <b>only</b> by identity-service.
 * Downstream services use {@link JwtDecoderFactory} exclusively.
 *
 * <p>Loads the RSA private key from the configured location
 * ({@code tradebridge.security.jwt.private-key-location}) — a PKCS#8 PEM.
 * Signs with {@code RS256} and puts {@code kid} in the header so verifiers
 * can rotate keys without breaking.
 */
public class JwtEncoderFactory {

    private final SecurityProperties.Jwt props;
    private final ResourceLoader resourceLoader;

    public JwtEncoderFactory(SecurityProperties properties, ResourceLoader resourceLoader) {
        this.props = Objects.requireNonNull(properties, "properties").getJwt();
        this.resourceLoader = Objects.requireNonNull(resourceLoader, "resourceLoader");
    }

    public JwtEncoder createEncoder() {
        PrivateKey privateKey = loadPrivateKey();
        String kid = props.getKeyId();
        String algorithm = props.getSigningAlgorithm();

        return parameters -> encode(parameters, privateKey, kid, algorithm);
    }

    private Jwt encode(JwtEncoderParameters parameters, PrivateKey key, String kid, String algorithm) {
        JwtClaimsSet claims = parameters.getClaims();
        JwsHeader headers = parameters.getJwsHeader();

        JwsHeader.Builder headerBuilder = JwsHeader.with(org.springframework.security.oauth2.jose.jws.SignatureAlgorithm.RS256);
        if (headers != null) {
            headers.getHeaders().forEach((k, v) -> {
                if (!"alg".equals(k)) {
                    headerBuilder.header(k, v);
                }
            });
        }
        if (kid != null) {
            headerBuilder.keyId(kid);
        }

        JwsHeader finalHeaders = headerBuilder.build();

        Instant now = Instant.now();
        Instant iat = claims.getIssuedAt() == null ? now : claims.getIssuedAt();
        Instant exp = claims.getExpiresAt() == null
                ? iat.plusSeconds(3600)
                : claims.getExpiresAt();
        String issuer = claims.getClaimAsString(JwtClaimNames.ISS);

        var builder = Jwts.builder()
                .header().keyId(kid).and()
                .issuer(issuer != null ? issuer : props.getIssuer())
                .audience().add(claims.getAudience() != null && !claims.getAudience().isEmpty()
                        ? claims.getAudience()
                        : java.util.List.of(props.getAudience())).and()
                .subject(claims.getSubject())
                .issuedAt(java.util.Date.from(iat))
                .expiration(java.util.Date.from(exp));

        if (claims.getId() != null) {
            builder.id(claims.getId());
        }
        claims.getClaims().forEach((k, v) -> {
            if (!"iss".equals(k) && !"sub".equals(k) && !"aud".equals(k)
                    && !"exp".equals(k) && !"iat".equals(k) && !"nbf".equals(k)) {
                builder.claim(k, v);
            }
        });
        if (claims.getNotBefore() != null) {
            builder.notBefore(java.util.Date.from(claims.getNotBefore()));
        }

        String token = builder.signWith(key, Jwts.SIG.RS256).compact();

        // Re-parse into a Spring Jwt so the caller has standard accessors.
        return Jwt.withTokenValue(token)
                .headers(h -> h.putAll(finalHeaders.getHeaders()))
                .claims(c -> c.putAll(claims.getClaims()))
                .issuedAt(iat)
                .expiresAt(exp)
                .build();
    }

    private PrivateKey loadPrivateKey() {
        String location = props.getPrivateKeyLocation();
        if (location == null || location.isBlank()) {
            throw new IllegalStateException(
                    "tradebridge.security.jwt.private-key-location is required to build a JwtEncoder");
        }
        try {
            Resource resource = resourceLoader.getResource(location);
            try (InputStream in = resource.getInputStream()) {
                String pem = new String(in.readAllBytes())
                        .replace("-----BEGIN PRIVATE KEY-----", "")
                        .replace("-----END PRIVATE KEY-----", "")
                        .replaceAll("\\s", "");
                byte[] der = Base64.getDecoder().decode(pem);
                PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(der);
                return KeyFactory.getInstance("RSA").generatePrivate(spec);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load private key from " + location, e);
        }
    }
}
