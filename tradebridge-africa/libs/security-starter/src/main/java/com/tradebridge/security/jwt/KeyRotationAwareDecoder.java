package com.tradebridge.security.jwt;

import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import java.security.interfaces.RSAPublicKey;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@link JwtDecoder} that resolves signing keys per-{@code kid} on each request.
 *
 * <p>Spring Security's built-in {@code NimbusJwtDecoder.withJwkSetUri()} works
 * fine for static JWKS, but does not transparently handle a rotation in which
 * the new key appears before the old one disappears *and* the client has cached
 * the old key. This decoder:
 * <ul>
 *   <li>delegates to {@link JwksProvider} which refreshes on unknown {@code kid};</li>
 *   <li>caches a per-{@code kid} {@link JwtDecoder};</li>
 *   <li>re-validates claims via {@link JwtClaimValidator}.</li>
 * </ul>
 */
public class KeyRotationAwareDecoder implements JwtDecoder {

    private final JwksProvider jwksProvider;
    private final JwtClaimValidator claimValidator;
    private final Map<String, JwtDecoder> decoderByKid = new ConcurrentHashMap<>();

    public KeyRotationAwareDecoder(JwksProvider jwksProvider, JwtClaimValidator claimValidator) {
        this.jwksProvider = Objects.requireNonNull(jwksProvider, "jwksProvider");
        this.claimValidator = Objects.requireNonNull(claimValidator, "claimValidator");
    }

    @Override
    public Jwt decode(String token) throws JwtException {
        String kid = extractKid(token);
        JwtDecoder decoder = decoderByKid.computeIfAbsent(kid, this::buildDecoder);
        Jwt jwt = decoder.decode(token);
        claimValidator.validate(jwt);
        return jwt;
    }

    private JwtDecoder buildDecoder(String kid) {
        RSAPublicKey publicKey = jwksProvider.getKey(kid);
        NimbusJwtDecoder decoder = NimbusJwtDecoder
                .withPublicKey(publicKey)
                .signatureAlgorithm(SignatureAlgorithm.RS256)
                .build();
        // Signature-only validation here; claim validation runs in decode()
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>());
        return decoder;
    }

    /**
     * Parses the {@code kid} from the JWT header without verifying the signature.
     * Extracting the header is safe; the actual signature check happens after.
     */
    private static String extractKid(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2) {
                throw new JwtException("Malformed JWT: expected 3 parts");
            }
            String headerJson = new String(java.util.Base64.getUrlDecoder().decode(parts[0]));
            var node = new com.fasterxml.jackson.databind.ObjectMapper().readTree(headerJson);
            String kid = node.path("kid").asText(null);
            if (kid == null || kid.isBlank()) {
                throw new JwtException("JWT header missing 'kid'");
            }
            return kid;
        } catch (JwtException e) {
            throw e;
        } catch (Exception e) {
            throw new JwtException("Failed to parse JWT header: " + e.getMessage(), e);
        }
    }

    /** For testing only. */
    public void clearCachedDecoders() {
        decoderByKid.clear();
    }
}
