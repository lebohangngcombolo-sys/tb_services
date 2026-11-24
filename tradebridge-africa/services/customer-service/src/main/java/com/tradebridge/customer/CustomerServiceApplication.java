package com.tradebridge.customer;

import com.tradebridge.customer.config.CustomerProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Customer Service — customer profiles, organizations, beneficial
 * ownership and KYC onboarding (spec §6).
 *
 * <p>Port 8084. Database {@code tb_customer_db}.
 * Publishes {@code customer.events}, consumes {@code identity.*} and
 * {@code country.*} events.
 *
 * <p>{@code @EnableScheduling} is required by the outbox relay.
 */
@SpringBootApplication(scanBasePackages = {"com.tradebridge.customer", "com.tradebridge"})
@EnableConfigurationProperties(CustomerProperties.class)
@EnableScheduling
public class CustomerServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CustomerServiceApplication.class, args);
    }
}