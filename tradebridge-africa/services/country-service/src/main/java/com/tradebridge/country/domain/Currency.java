package com.tradebridge.country.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.Objects;

/**
 * ISO 4217 currency catalogue (spec §4.2 {@code currencies}).
 *
 * <p>Natural key: the ISO code itself. Rows are seeded via Flyway and
 * modified only through admin endpoints.
 */
@Entity
@Table(name = "currencies")
public class Currency {

    @Id
    @Column(name = "code", nullable = false, updatable = false, length = 3)
    private String code;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "symbol", length = 10)
    private String symbol;

    @Column(name = "decimal_places", nullable = false)
    private short decimalPlaces;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    protected Currency() { }

    public Currency(String code, String name, String symbol, short decimalPlaces, boolean enabled) {
        this.code = Objects.requireNonNull(code, "code").toUpperCase();
        this.name = Objects.requireNonNull(name, "name");
        this.symbol = symbol;
        this.decimalPlaces = decimalPlaces;
        this.enabled = enabled;
    }

    public void updateDetails(String name, String symbol, Short decimalPlaces) {
        if (name != null && !name.isBlank()) this.name = name;
        if (symbol != null) this.symbol = symbol;
        if (decimalPlaces != null) this.decimalPlaces = decimalPlaces;
    }

    public void enable() { this.enabled = true; }
    public void disable() { this.enabled = false; }

    public String getCode() { return code; }
    public String getName() { return name; }
    public String getSymbol() { return symbol; }
    public short getDecimalPlaces() { return decimalPlaces; }
    public boolean isEnabled() { return enabled; }
}