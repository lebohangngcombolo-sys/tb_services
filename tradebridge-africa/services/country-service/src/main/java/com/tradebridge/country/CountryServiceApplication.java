package com.tradebridge.country;

import com.tradebridge.country.config.CountryProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Country Service — authoritative source for countries, currencies,
 * languages and country-specific onboarding configuration (spec §4).
 *
 * <p>Port 8082. Database {@code tb_country_db}. Publishes {@code country.events}.
 *
 * <p>{@code @EnableScheduling} is required by the outbox relay;
 * {@code @EnableCaching} enables Redis-backed read caching for public
 * endpoints.
 */
@SpringBootApplication(scanBasePackages = {"com.tradebridge.country", "com.tradebridge"})
@EnableConfigurationProperties(CountryProperties.class)
@EnableScheduling
@EnableCaching
public class CountryServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CountryServiceApplication.class, args);
    }
}
