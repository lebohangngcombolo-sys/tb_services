package com.tradebridge.observability.tracing;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Adds TradeBridge-specific observation filtering to Spring Boot's
 * Micrometer tracing auto-configuration.
 */
@Configuration(proxyBeanMethods = false)
public class OpenTelemetryConfig {

    @Bean
    public io.micrometer.observation.ObservationPredicate tradebridgeObservationPredicate() {
        return (name, context) ->
                name == null || !name.startsWith("http.server.requests.actuator");
    }
}
