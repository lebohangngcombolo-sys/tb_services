package com.tradebridge.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.Objects;

/**
 * Standard success envelope per spec §1.2.
 *
 * <pre>
 * {
 *   "success": true,
 *   "message": "Operation completed successfully",
 *   "data": {},
 *   "timestamp": "2026-11-11T10:00:00Z",
 *   "correlationId": "uuid"
 * }
 * </pre>
 *
 * @param <T> payload type
 */
@JsonInclude(JsonInclude.Include.ALWAYS)
public final class ApiResponse<T> {

    @JsonProperty("success")
    private final boolean success;

    @JsonProperty("message")
    private final String message;

    @JsonProperty("data")
    private final T data;

    @JsonProperty("timestamp")
    private final Instant timestamp;

    @JsonProperty("correlationId")
    private final String correlationId;

    private ApiResponse(boolean success, String message, T data, Instant timestamp, String correlationId) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.timestamp = Objects.requireNonNull(timestamp, "timestamp");
        this.correlationId = correlationId;
    }

    public static <T> ApiResponse<T> success(T data, String correlationId) {
        return new ApiResponse<>(true, "Operation completed successfully", data, Instant.now(), correlationId);
    }

    public static <T> ApiResponse<T> success(T data, String message, String correlationId) {
        return new ApiResponse<>(true, message, data, Instant.now(), correlationId);
    }

    public static ApiResponse<Void> success(String correlationId) {
        return new ApiResponse<>(true, "Operation completed successfully", null, Instant.now(), correlationId);
    }

    public static ApiResponse<Void> success(String message, String correlationId) {
        return new ApiResponse<>(true, message, null, Instant.now(), correlationId);
    }

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public T getData() { return data; }
    public Instant getTimestamp() { return timestamp; }
    public String getCorrelationId() { return correlationId; }
}