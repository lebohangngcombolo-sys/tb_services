package com.tradebridge.identity.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Explicit JPA wiring for the identity service. Scans only our package so
 * that stray entities from other modules (if any are ever added) do not
 * leak into this persistence unit.
 */
@Configuration
@EnableTransactionManagement
@EntityScan(basePackages = "com.tradebridge.identity.domain")
@EnableJpaRepositories(basePackages = "com.tradebridge.identity.repository")
public class PersistenceConfig {
}
