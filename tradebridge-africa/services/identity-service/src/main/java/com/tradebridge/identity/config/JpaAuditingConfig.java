package com.tradebridge.identity.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Enables {@code @CreatedDate}/{@code @LastModifiedDate} style auditing.
 *
 * <p>The domain entities currently set timestamps in `@PrePersist`/`@PreUpdate`
 * explicitly, so JPA auditing is not strictly required. It is enabled to allow
 * new entities to opt in without wiring changes.
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
