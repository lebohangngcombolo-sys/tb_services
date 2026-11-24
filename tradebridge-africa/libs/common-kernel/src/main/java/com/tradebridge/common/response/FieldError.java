package com.tradebridge.common.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * Field-level validation error entry, per spec §1.2:
 *
 * <pre>
 * { "field": "email", "message": "Invalid email address" }
 * </pre>
 */
public record FieldError(
        @JsonProperty("field") String field,
        @JsonProperty("message") String message
) {
    public FieldError {
        Objects.requireNonNull(field, "field");
        Objects.requireNonNull(message, "message");
    }
}