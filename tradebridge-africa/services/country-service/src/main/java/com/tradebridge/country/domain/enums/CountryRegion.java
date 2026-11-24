package com.tradebridge.country.domain.enums;

import java.util.Arrays;
import java.util.List;

/**
 * Geographic region classification (spec §4.2 {@code countries.region}).
 *
 * <p>Not a business rule — purely for reporting and organization.
 * Actual onboarding rules are stored per country in the
 * {@code organization_requirements} / {@code kyc_requirements} tables.
 */
public enum CountryRegion {
    AFRICA,
    EUROPE,
    NORTH_AMERICA,
    SOUTH_AMERICA,
    ASIA,
    OCEANIA,
    MIDDLE_EAST,
    CARIBBEAN,
    OTHER;

    /** Case-insensitive lookup that returns {@code OTHER} on unknown input. */
    public static CountryRegion fromString(String value) {
        if (value == null || value.isBlank()) {
            return OTHER;
        }
        String normalized = value.trim().toUpperCase().replace(' ', '_').replace('-', '_');
        return Arrays.stream(values())
                .filter(r -> r.name().equals(normalized))
                .findFirst()
                .orElse(OTHER);
    }

    /** Human-readable values for UIs. */
    public static List<String> allNames() {
        return Arrays.stream(values()).map(Enum::name).toList();
    }
}