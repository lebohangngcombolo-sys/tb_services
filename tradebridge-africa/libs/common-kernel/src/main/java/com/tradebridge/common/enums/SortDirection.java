package com.tradebridge.common.enums;

/**
 * Sort direction for paged queries.
 */
public enum SortDirection {
    ASC,
    DESC;

    public static SortDirection fromString(String value) {
        if (value == null || value.isBlank()) {
            return ASC;
        }
        return switch (value.trim().toUpperCase()) {
            case "ASC" -> ASC;
            case "DESC" -> DESC;
            default -> throw new IllegalArgumentException("Unsupported sort direction: " + value);
        };
    }
}