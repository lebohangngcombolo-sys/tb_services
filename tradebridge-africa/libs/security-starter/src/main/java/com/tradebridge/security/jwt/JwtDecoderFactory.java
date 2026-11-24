package com.tradebridge.security.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradebridge.security.config.SecurityProperties;
import org.springframework.core.io.ResourceLoader;
import org.springframework.security.oauth2.jwt.JwtDecoder;

/**
 * Builds the {@link JwtDecoder} chain used by downstream services
 * (identity, country, notification, customer).
 *
 * <p>Returns a {@link KeyRotationAwareDecoder} that:
 * <ol>
 *   <li>resolves keys via {@link JwksProvider} (JWKS URI or static file),</li>
 *   <li>verifies the signature per request,</li>
 *   <li>validates claims via {@link JwtClaimValidator}.</li>
 * </ol>
 */
public class JwtDecoderFactory {

    private final SecurityProperties properties;
    private final ResourceLoader resourceLoader;
    private final ObjectMapper objectMapper;

    public JwtDecoderFactory(SecurityProperties properties,
                             ResourceLoader resourceLoader,
                             ObjectMapper objectMapper) {
        this.properties = properties;
        this.resourceLoader = resourceLoader;
        this.objectMapper = objectMapper;
    }

    public JwksProvider createJwksProvider() {
        return new JwksProvider(properties.getJwt(), resourceLoader, objectMapper);
    }

    public JwtClaimValidator createClaimValidator() {
        return new JwtClaimValidator(properties.getJwt());
    }

    public JwtDecoder createDecoder() {
        return new KeyRotationAwareDecoder(createJwksProvider(), createClaimValidator());
    }
}
