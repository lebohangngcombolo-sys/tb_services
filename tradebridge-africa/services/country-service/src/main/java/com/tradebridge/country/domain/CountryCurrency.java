package com.tradebridge.country.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.util.Objects;
import java.util.UUID;

/**
 * Country ↔ Currency mapping (spec §4.2 {@code country_currencies}).
 *
 * <p>Exactly one currency per country carries {@code is_default = TRUE}.
 * Enforcement is in the service layer (spec §4.4); the DB enforces
 * uniqueness of the (country, currency) pair only.
 */
@Entity
@Table(name = "country_currencies",
        uniqueConstraints = @UniqueConstraint(name = "uk_country_currencies",
                columnNames = {"country_code", "currency_code"}))
public class CountryCurrency {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "country_code", nullable = false, length = 2)
    private String countryCode;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;

    @Column(name = "is_default", nullable = false)
    private boolean isDefault;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    protected CountryCurrency() { }

    public CountryCurrency(UUID id, String countryCode, String currencyCode,
                           boolean isDefault, boolean enabled) {
        this.id = Objects.requireNonNull(id, "id");
        this.countryCode = Objects.requireNonNull(countryCode, "countryCode").toUpperCase();
        this.currencyCode = Objects.requireNonNull(currencyCode, "currencyCode").toUpperCase();
        this.isDefault = isDefault;
        this.enabled = enabled;
    }

    public void setDefault(boolean v) { this.isDefault = v; }
    public void setEnabled(boolean v) { this.enabled = v; }

    public UUID getId() { return id; }
    public String getCountryCode() { return countryCode; }
    public String getCurrencyCode() { return currencyCode; }
    public boolean isDefault() { return isDefault; }
    public boolean isEnabled() { return enabled; }
}