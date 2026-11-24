package com.tradebridge.country.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Enables JPA auditing.
 *
 * <p>The current entities set timestamps in {@code @PrePersist}/{@code @PreUpdate}
 * explicitly, so auditing is not required for them — but it is enabled so
 * future entities can opt in with {@code @CreatedDate}/{@code @LastModifiedDate}.
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}