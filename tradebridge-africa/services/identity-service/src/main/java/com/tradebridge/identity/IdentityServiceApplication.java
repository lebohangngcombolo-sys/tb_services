package com.tradebridge.identity;

import com.tradebridge.identity.config.IdentityProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Identity Service — user accounts, authentication, authorization and MFA.
 *
 * <p>Port 8081. Database {@code tb_identity_db}. Publishes {@code identity.events},
 * consumes {@code notification.delivery.failed.v1} and
 * {@code country.configuration.updated.v1}.
 *
 * <p>{@code @EnableScheduling} is required by the messaging-starter's
 * {@code OutboxRelay}.
 */
@SpringBootApplication(scanBasePackages = {"com.tradebridge.identity", "com.tradebridge"})
@EnableConfigurationProperties(IdentityProperties.class)
@EnableScheduling
public class IdentityServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(IdentityServiceApplication.class, args);
    }
}
