package com.tradebridge.country.domain.enums;

/**
 * Lifecycle of a country configuration version (spec §4.2
 * {@code configuration_versions} "Suggested statuses").
 *
 * <p>State transitions:
 * <pre>
 *   DRAFT → PUBLISHED → SUPERSEDED  (when a newer version is published)
 *                    ↘ ROLLED_BACK  (when this version is restored after being superseded)
 * </pre>
 *
 * <p>Once a version leaves DRAFT, its content is immutable (§4.4
 * "Published configuration versions must be immutable; changes create
 * a new version").
 */
public enum ConfigurationStatus {
    DRAFT,
    PUBLISHED,
    SUPERSEDED,
    ROLLED_BACK
}