package com.tradebridge.customer.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Enables JPA auditing for future entities. Current entities set
 * timestamps in {@code @PrePersist}/{@code @PreUpdate} explicitly.
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}