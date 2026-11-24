package com.tradebridge.country.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.Objects;

/**
 * Language catalogue (spec §4.2 {@code languages}).
 * Initial seed: EN (English), PT (Portuguese).
 */
@Entity
@Table(name = "languages")
public class Language {

    @Id
    @Column(name = "code", nullable = false, updatable = false, length = 5)
    private String code;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "native_name", length = 100)
    private String nativeName;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    protected Language() { }

    public Language(String code, String name, String nativeName, boolean enabled) {
        this.code = Objects.requireNonNull(code, "code").toLowerCase();
        this.name = Objects.requireNonNull(name, "name");
        this.nativeName = nativeName;
        this.enabled = enabled;
    }

    public void updateDetails(String name, String nativeName) {
        if (name != null && !name.isBlank()) this.name = name;
        if (nativeName != null) this.nativeName = nativeName;
    }

    public void enable() { this.enabled = true; }
    public void disable() { this.enabled = false; }

    public String getCode() { return code; }
    public String getName() { return name; }
    public String getNativeName() { return nativeName; }
    public boolean isEnabled() { return enabled; }
}