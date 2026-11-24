package com.tradebridge.security.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradebridge.security.jwt.JwtDecoderFactory;
import com.tradebridge.security.jwt.JwtEncoderFactory;
import com.tradebridge.security.jwt.JwksProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ResourceLoader;
import org.springframework.security.oauth2.jwt.JwtDecoder;

/**
 * JWT infrastructure auto-configuration.
 *
 * <p>Registers a {@link JwksProvider} when {@code tradebridge.security.jwt.jwks-uri}
 * is set, and a fully wired {@link JwtDecoder} when either {@code jwks-uri} or
 * {@code public-key-location} is set.
 *
 * <p>Services that use a static public key still get the decoder because the
 * {@link JwtDecoderFactory} handles both modes.
 *
 * <p>The Gateway MUST exclude this configuration and build its own reactive
 * decoder from {@link JwtDecoderFactory#createJwksProvider()} directly.
 */
@AutoConfiguration
@ConditionalOnClass(name = "org.springframework.security.oauth2.jwt.JwtDecoder")
public class JwtAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "tradebridge.security.jwt", name = "private-key-location")
    public JwtEncoderFactory jwtEncoderFactory(SecurityProperties properties,
                                               ResourceLoader resourceLoader) {
        return new JwtEncoderFactory(properties, resourceLoader);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "tradebridge.security.jwt", name = "jwks-uri")
    public JwksProvider jwksProvider(SecurityProperties properties,
                                     ResourceLoader resourceLoader,
                                     ObjectMapper objectMapper) {
        return new JwksProvider(properties.getJwt(), resourceLoader, objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "tradebridge.security.jwt", name = "jwks-uri")
    public JwtDecoder jwtDecoder(SecurityProperties properties,
                                 ResourceLoader resourceLoader,
                                 ObjectMapper objectMapper) {
        return new JwtDecoderFactory(properties, resourceLoader, objectMapper).createDecoder();
    }

    /**
     * Static-file mode: services that set {@code public-key-location} only get a
     * decoder via {@link JwtDecoderFactory}, but the base JWT decoder bean is
     * registered here because many Spring Security auto-configurations require one.
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "tradebridge.security.jwt", name = "public-key-location")
    public JwtDecoder staticKeyJwtDecoder(SecurityProperties properties,
                                          ResourceLoader resourceLoader,
                                          ObjectMapper objectMapper) {
        return new JwtDecoderFactory(properties, resourceLoader, objectMapper).createDecoder();
    }
}
