package com.tradebridge.gateway;

import com.tradebridge.gateway.config.GatewayProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * API Gateway (spec §2). Reactive Spring Cloud Gateway instance.
 *
 * <p>Port 8080. No database. No business Kafka producer — all
 * non-JSON side effects are telemetry (logs, metrics, traces).
 *
 * <p>{@code scanBasePackages} limited to {@code com.tradebridge.gateway}
 * plus the {@code common-kernel} beans we need (there are none —
 * auto-configuration handles common-kernel). The security-starter's
 * {@code JwtAutoConfiguration} is excluded at YAML level (see
 * {@code application.yml}) because the Gateway builds its own reactive
 * decoder.
 */
@SpringBootApplication(scanBasePackages = "com.tradebridge.gateway")
@EnableConfigurationProperties(GatewayProperties.class)
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}