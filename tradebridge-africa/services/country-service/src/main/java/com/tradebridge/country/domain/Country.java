package com.tradebridge.country.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Country aggregate (spec §4.2 {@code countries}).
 *
 * <p>Owns its currencies and languages through join tables; those are
 * managed by their respective services, not cascaded from this entity.
 */
@Entity
@Table(name = "countries",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_countries_iso2", columnNames = "iso2_code"),
                @UniqueConstraint(name = "uk_countries_iso3", columnNames = "iso3_code")
        })
public class Country {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "iso2_code", nullable = false, length = 2)
    private String iso2Code;

    @Column(name = "iso3_code", nullable = false, length = 3)
    private String iso3Code;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "region", length = 100)
    private String region;

    @Column(name = "calling_code", length = 10)
    private String callingCode;

    @Column(name = "default_currency_code", nullable = false, length = 3)
    private String defaultCurrencyCode;

    @Column(name = "default_language_code", nullable = false, length = 5)
    private String defaultLanguageCode;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "row_version", nullable = false)
    private long rowVersion;

    protected Country() { }

    public Country(UUID id, String iso2Code, String iso3Code, String name,
                   String region, String callingCode,
                   String defaultCurrencyCode, String defaultLanguageCode,
                   boolean enabled) {
        this.id = Objects.requireNonNull(id, "id");
        this.iso2Code = Objects.requireNonNull(iso2Code, "iso2Code").toUpperCase();
        this.iso3Code = Objects.requireNonNull(iso3Code, "iso3Code").toUpperCase();
        this.name = Objects.requireNonNull(name, "name");
        this.region = region;
        this.callingCode = callingCode;
        this.defaultCurrencyCode = Objects.requireNonNull(defaultCurrencyCode, "defaultCurrencyCode").toUpperCase();
        this.defaultLanguageCode = Objects.requireNonNull(defaultLanguageCode, "defaultLanguageCode").toUpperCase();
        this.enabled = enabled;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    // ------------------------------------------------------------------ behavior

    public void updateDetails(String name, String region, String callingCode,
                              String defaultCurrencyCode, String defaultLanguageCode) {
        if (name != null && !name.isBlank()) this.name = name;
        if (region != null) this.region = region;
        if (callingCode != null) this.callingCode = callingCode;
        if (defaultCurrencyCode != null && !defaultCurrencyCode.isBlank()) {
            this.defaultCurrencyCode = defaultCurrencyCode.toUpperCase();
        }
        if (defaultLanguageCode != null && !defaultLanguageCode.isBlank()) {
            this.defaultLanguageCode = defaultLanguageCode.toUpperCase();
        }
    }

    public void enable() {
        this.enabled = true;
    }

    public void disable() {
        this.enabled = false;
    }

    // ------------------------------------------------------------------ getters

    public UUID getId() { return id; }
    public String getIso2Code() { return iso2Code; }
    public String getIso3Code() { return iso3Code; }
    public String getName() { return name; }
    public String getRegion() { return region; }
    public String getCallingCode() { return callingCode; }
    public String getDefaultCurrencyCode() { return defaultCurrencyCode; }
    public String getDefaultLanguageCode() { return defaultLanguageCode; }
    public boolean isEnabled() { return enabled; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public long getRowVersion() { return rowVersion; }
}