package com.tradebridge.common.exception;

import com.tradebridge.common.response.FieldError;

import java.util.Collections;
import java.util.List;

/**
 * Base class for all domain/business exceptions raised inside a service.
 * The API layer should translate these into the standard {@code ErrorResponse}
 * envelope using {@link ErrorCode}.
 */
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;
    private final List<FieldError> fieldErrors;

    public BusinessException(ErrorCode errorCode, String message) {
        this(errorCode, message, Collections.emptyList());
    }

    public BusinessException(ErrorCode errorCode, String message, List<FieldError> fieldErrors) {
        super(message);
        this.errorCode = errorCode == null ? ErrorCode.INTERNAL_ERROR : errorCode;
        this.fieldErrors = fieldErrors == null ? Collections.emptyList() : List.copyOf(fieldErrors);
    }

    public BusinessException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode == null ? ErrorCode.INTERNAL_ERROR : errorCode;
        this.fieldErrors = Collections.emptyList();
    }

    public ErrorCode getErrorCode() { return errorCode; }
    public List<FieldError> getFieldErrors() { return fieldErrors; }
}