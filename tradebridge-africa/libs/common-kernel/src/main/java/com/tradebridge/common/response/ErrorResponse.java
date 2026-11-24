package com.tradebridge.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tradebridge.common.exception.ErrorCode;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Standard error envelope per spec §1.2:
 *
 * <pre>
 * {
 *   "success": false,
 *   "code": "VALIDATION_ERROR",
 *   "message": "The submitted information is invalid",
 *   "errors": [ { "field": "email", "message": "Invalid email address" } ],
 *   "timestamp": "2026-11-11T10:00:00Z",
 *   "correlationId": "uuid"
 * }
 * </pre>
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public final class ErrorResponse {

    @JsonProperty("success")
    private final boolean success;

    @JsonProperty("code")
    private final String code;

    @JsonProperty("message")
    private final String message;

    @JsonProperty("errors")
    private final List<FieldError> errors;

    @JsonProperty("timestamp")
    private final Instant timestamp;

    @JsonProperty("correlationId")
    private final String correlationId;

    private ErrorResponse(String code, String message, List<FieldError> errors, Instant timestamp, String correlationId) {
        this.success = false;
        this.code = Objects.requireNonNull(code, "code");
        this.message = Objects.requireNonNull(message, "message");
        this.errors = errors == null ? Collections.emptyList() : List.copyOf(errors);
        this.timestamp = Objects.requireNonNull(timestamp, "timestamp");
        this.correlationId = correlationId;
    }

    public static ErrorResponse of(ErrorCode errorCode, String message, String correlationId) {
        return new ErrorResponse(errorCode.name(), message, Collections.emptyList(), Instant.now(), correlationId);
    }

    public static ErrorResponse of(ErrorCode errorCode, String message, List<FieldError> errors, String correlationId) {
        return new ErrorResponse(errorCode.name(), message, errors, Instant.now(), correlationId);
    }

    public static ErrorResponse of(String code, String message, List<FieldError> errors, String correlationId) {
        return new ErrorResponse(code, message, errors, Instant.now(), correlationId);
    }

    public boolean isSuccess() { return success; }
    public String getCode() { return code; }
    public String getMessage() { return message; }
    public List<FieldError> getErrors() { return errors; }
    public Instant getTimestamp() { return timestamp; }
    public String getCorrelationId() { return correlationId; }
}