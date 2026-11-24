package com.tradebridge.country.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableTransactionManagement
@EntityScan(basePackages = "com.tradebridge.country.domain")
@EnableJpaRepositories(basePackages = "com.tradebridge.country.repository")
public class PersistenceConfig {
}