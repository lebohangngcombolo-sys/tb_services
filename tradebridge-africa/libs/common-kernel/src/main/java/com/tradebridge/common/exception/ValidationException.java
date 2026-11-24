package com.tradebridge.common.exception;

import com.tradebridge.common.response.FieldError;

import java.util.List;

/**
 * Raised when request payload validation fails inside the service layer.
 * For framework-level Bean Validation, controllers translate
 * {@code MethodArgumentNotValidException} into this type.
 */
public class ValidationException extends BusinessException {

    public ValidationException(String message) {
        super(ErrorCode.VALIDATION_ERROR, message);
    }

    public ValidationException(String message, List<FieldError> fieldErrors) {
        super(ErrorCode.VALIDATION_ERROR, message, fieldErrors);
    }

    public static ValidationException singleField(String field, String message) {
        return new ValidationException("The submitted information is invalid",
                List.of(new FieldError(field, message)));
    }
}