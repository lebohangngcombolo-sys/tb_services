package com.tradebridge.country.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.util.Objects;
import java.util.UUID;

/**
 * Country ↔ Language mapping (spec §4.2 {@code country_languages}).
 */
@Entity
@Table(name = "country_languages",
        uniqueConstraints = @UniqueConstraint(name = "uk_country_languages",
                columnNames = {"country_code", "language_code"}))
public class CountryLanguage {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "country_code", nullable = false, length = 2)
    private String countryCode;

    @Column(name = "language_code", nullable = false, length = 5)
    private String languageCode;

    @Column(name = "is_default", nullable = false)
    private boolean isDefault;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    protected CountryLanguage() { }

    public CountryLanguage(UUID id, String countryCode, String languageCode,
                           boolean isDefault, boolean enabled) {
        this.id = Objects.requireNonNull(id, "id");
        this.countryCode = Objects.requireNonNull(countryCode, "countryCode").toUpperCase();
        this.languageCode = Objects.requireNonNull(languageCode, "languageCode").toLowerCase();
        this.isDefault = isDefault;
        this.enabled = enabled;
    }

    public void setDefault(boolean v) { this.isDefault = v; }
    public void setEnabled(boolean v) { this.enabled = v; }

    public UUID getId() { return id; }
    public String getCountryCode() { return countryCode; }
    public String getLanguageCode() { return languageCode; }
    public boolean isDefault() { return isDefault; }
    public boolean isEnabled() { return enabled; }
}