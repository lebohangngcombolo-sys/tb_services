package com.tradebridge.gateway.config;

import com.tradebridge.security.jwt.JwksProvider;
import com.tradebridge.security.jwt.JwtClaimValidator;
import com.tradebridge.security.jwt.KeyRotationAwareDecoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Reactive-safe JWT decoding for the Gateway (spec §2.2, §11 decision 1).
 *
 * <p>The Gateway reuses the security-starter's {@link JwksProvider},
 * {@link JwtClaimValidator}, and {@link KeyRotationAwareDecoder} to obtain
 * a plain {@link JwtDecoder} bean. WebFlux wraps it via
 * {@code ReactiveJwtDecoder} internally when it sees a decoder bean.
 *
 * <p>Asymmetric verification against identity-service's
 * {@code /.well-known/jwks.json}. Key rotation is handled by
 * {@code KeyRotationAwareDecoder}.
 */
@Configuration
public class JwtGatewayConfig {

    @Bean
    public JwtDecoder gatewayJwtDecoder(GatewayProperties gatewayProperties,
                                         ResourceLoader resourceLoader,
                                         ObjectMapper objectMapper) {
        // Map gateway JWT config to the security-starter's expected shape
        var securityProps = new com.tradebridge.security.config.SecurityProperties();
        var jwt = securityProps.getJwt();
        jwt.setIssuer(gatewayProperties.getJwt().getIssuer());
        jwt.setAudience(gatewayProperties.getJwt().getAudience());
        jwt.setJwksUri(gatewayProperties.getJwt().getJwksUri());
        jwt.setPublicKeyLocation(gatewayProperties.getJwt().getPublicKeyLocation());
        jwt.setKeyId(gatewayProperties.getJwt().getKeyId());
        jwt.setClockSkew(gatewayProperties.getJwt().getClockSkew());

        JwksProvider jwksProvider = new JwksProvider(jwt, resourceLoader, objectMapper);
        JwtClaimValidator claimValidator = new JwtClaimValidator(jwt);
        return new KeyRotationAwareDecoder(jwksProvider, claimValidator);
    }
}