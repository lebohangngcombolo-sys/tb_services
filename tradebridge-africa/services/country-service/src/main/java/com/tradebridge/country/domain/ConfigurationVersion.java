package com.tradebridge.country.domain;

import com.tradebridge.country.domain.enums.ConfigurationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Version header for a published/draft set of {@link CountryConfiguration}
 * rows (spec §4.2 {@code configuration_versions}).
 *
 * <p>Every value row in {@code country_configurations} belongs to exactly
 * one version. The header carries the version status and audit trail.
 */
@Entity
@Table(name = "configuration_versions",
        uniqueConstraints = @UniqueConstraint(name = "uk_config_versions",
                columnNames = {"country_code", "version"}))
public class ConfigurationVersion {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "country_code", nullable = false, length = 2)
    private String countryCode;

    @Column(name = "version", nullable = false)
    private int version;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ConfigurationStatus status;

    @Column(name = "change_summary", columnDefinition = "text")
    private String changeSummary;

    @Column(name = "published_by")
    private UUID publishedBy;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ConfigurationVersion() { }

    public ConfigurationVersion(UUID id, String countryCode, int version,
                                ConfigurationStatus status, String changeSummary) {
        this.id = Objects.requireNonNull(id, "id");
        this.countryCode = Objects.requireNonNull(countryCode, "countryCode").toUpperCase();
        this.version = version;
        this.status = Objects.requireNonNull(status, "status");
        this.changeSummary = changeSummary;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }

    // ------------------------------------------------------------------ behavior

    public void publish(UUID publishedBy, String changeSummary) {
        if (this.status != ConfigurationStatus.DRAFT) {
            throw new IllegalStateException("Only DRAFT versions can be published");
        }
        this.status = ConfigurationStatus.PUBLISHED;
        this.publishedBy = publishedBy;
        this.publishedAt = Instant.now();
        if (changeSummary != null) this.changeSummary = changeSummary;
    }

    public void supersede() {
        if (this.status == ConfigurationStatus.PUBLISHED) {
            this.status = ConfigurationStatus.SUPERSEDED;
        }
    }

    public void markRolledBack() {
        this.status = ConfigurationStatus.ROLLED_BACK;
    }

    // ------------------------------------------------------------------ getters

    public UUID getId() { return id; }
    public String getCountryCode() { return countryCode; }
    public int getVersion() { return version; }
    public ConfigurationStatus getStatus() { return status; }
    public String getChangeSummary() { return changeSummary; }
    public UUID getPublishedBy() { return publishedBy; }
    public Instant getPublishedAt() { return publishedAt; }
    public Instant getCreatedAt() { return createdAt; }
}