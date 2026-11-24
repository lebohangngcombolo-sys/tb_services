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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Key-value configuration entry (spec §4.2 {@code country_configurations}).
 *
 * <p>Each configuration row is one key for a country. Rows are immutable
 * once published; a new version creates new rows.
 *
 * <p>The uniqueness rule {@code (country_code, configuration_key, version)}
 * ensures at most one value per key per version.
 */
@Entity
@Table(name = "country_configurations",
        uniqueConstraints = @UniqueConstraint(name = "uk_country_config",
                columnNames = {"country_code", "configuration_key", "version"}))
public class CountryConfiguration {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "country_code", nullable = false, length = 2)
    private String countryCode;

    @Column(name = "configuration_key", nullable = false, length = 150)
    private String configurationKey;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "configuration_value", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> configurationValue;

    @Column(name = "version", nullable = false)
    private int version;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ConfigurationStatus status;

    @Column(name = "effective_from")
    private Instant effectiveFrom;

    @Column(name = "effective_until")
    private Instant effectiveUntil;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected CountryConfiguration() { }

    public CountryConfiguration(UUID id, String countryCode, String configurationKey,
                                Map<String, Object> configurationValue, int version,
                                ConfigurationStatus status, Instant effectiveFrom,
                                Instant effectiveUntil, UUID createdBy) {
        this.id = Objects.requireNonNull(id, "id");
        this.countryCode = Objects.requireNonNull(countryCode, "countryCode").toUpperCase();
        this.configurationKey = Objects.requireNonNull(configurationKey, "configurationKey");
        this.configurationValue = Objects.requireNonNull(configurationValue, "configurationValue");
        this.version = version;
        this.status = Objects.requireNonNull(status, "status");
        this.effectiveFrom = effectiveFrom;
        this.effectiveUntil = effectiveUntil;
        this.createdBy = createdBy;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public String getCountryCode() { return countryCode; }
    public String getConfigurationKey() { return configurationKey; }
    public Map<String, Object> getConfigurationValue() { return configurationValue; }
    public int getVersion() { return version; }
    public ConfigurationStatus getStatus() { return status; }
    public Instant getEffectiveFrom() { return effectiveFrom; }
    public Instant getEffectiveUntil() { return effectiveUntil; }
    public UUID getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
}