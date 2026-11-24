package com.tradebridge.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

/**
 * CORS configuration (spec §2.5 "Validate CORS origins against configured allowlists").
 *
 * <p>If no origins are configured, CORS is effectively disabled — safe default
 * for a backend-only deployment. In local/prod, set
 * {@code tradebridge.gateway.cors.allowed-origins}.
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsWebFilter corsWebFilter(GatewayProperties properties) {
        GatewayProperties.Cors cors = properties.getCors();
        if (cors.getAllowedOrigins() == null || cors.getAllowedOrigins().isEmpty()) {
            // Return a filter with an empty configuration — effectively no CORS
            UrlBasedCorsConfigurationSource emptySource = new UrlBasedCorsConfigurationSource();
            CorsConfiguration emptyConfig = new CorsConfiguration();
            emptySource.registerCorsConfiguration("/**", emptyConfig);
            return new CorsWebFilter(emptySource);
        }
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(cors.getAllowedOrigins());
        config.setAllowedMethods(cors.getAllowedMethods());
        config.setAllowedHeaders(cors.getAllowedHeaders());
        config.setExposedHeaders(cors.getExposedHeaders());
        config.setAllowCredentials(cors.isAllowCredentials());
        config.setMaxAge(cors.getMaxAge());

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsWebFilter(source);
    }
}