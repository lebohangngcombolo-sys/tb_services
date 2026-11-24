package com.tradebridge.common.id;

import java.util.UUID;

/**
 * Small helpers for identifiers used in requests and events.
 */
public final class IdValidator {

    private IdValidator() { }

    public static boolean isValidUuid(String candidate) {
        if (candidate == null || candidate.isBlank()) {
            return false;
        }
        try {
            UUID.fromString(candidate.trim());
            return true;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    public static UUID parse(String candidate) {
        if (!isValidUuid(candidate)) {
            throw new IllegalArgumentException("Invalid UUID: " + candidate);
        }
        return UUID.fromString(candidate.trim());
    }
}