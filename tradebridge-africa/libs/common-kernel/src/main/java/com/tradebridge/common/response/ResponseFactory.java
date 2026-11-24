package com.tradebridge.common.response;

import com.tradebridge.common.context.CorrelationContext;
import com.tradebridge.common.exception.BusinessException;
import com.tradebridge.common.exception.ErrorCode;
import com.tradebridge.common.pagination.PageMetadata;

import java.util.List;

/**
 * Convenience factory that automatically reads the current correlation ID
 * from {@link CorrelationContext}. This is what controllers should use,
 * so callers never need to pass the correlationId manually.
 */
public final class ResponseFactory {

    private ResponseFactory() { }

    // ------------------------------------------------------------------ success

    public static <T> ApiResponse<T> ok(T data) {
        return ApiResponse.success(data, CorrelationContext.currentOrGenerate());
    }

    public static <T> ApiResponse<T> ok(T data, String message) {
        return ApiResponse.success(data, message, CorrelationContext.currentOrGenerate());
    }

    public static ApiResponse<Void> ok() {
        return ApiResponse.success(CorrelationContext.currentOrGenerate());
    }

    public static ApiResponse<Void> ok(String message) {
        return ApiResponse.success(message, CorrelationContext.currentOrGenerate());
    }

    public static <T> PageResponse<T> page(List<T> content, PageMetadata page) {
        return PageResponse.of(content, page);
    }

    // -------------------------------------------------------------------- error

    public static ErrorResponse error(ErrorCode code, String message) {
        return ErrorResponse.of(code, message, CorrelationContext.currentOrGenerate());
    }

    public static ErrorResponse error(ErrorCode code, String message, List<FieldError> fieldErrors) {
        return ErrorResponse.of(code, message, fieldErrors, CorrelationContext.currentOrGenerate());
    }

    public static ErrorResponse error(BusinessException ex) {
        return ErrorResponse.of(ex.getErrorCode(), ex.getMessage(), ex.getFieldErrors(),
                CorrelationContext.currentOrGenerate());
    }
}