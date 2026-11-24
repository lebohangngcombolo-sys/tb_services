
package com.tradebridge.country.config;

import com.tradebridge.common.exception.BusinessException;
import com.tradebridge.common.exception.ErrorCode;
import com.tradebridge.common.exception.ForbiddenException;
import com.tradebridge.common.exception.UnauthorizedException;
import com.tradebridge.common.exception.ValidationException;
import com.tradebridge.common.response.ErrorResponse;
import com.tradebridge.common.response.FieldError;
import com.tradebridge.common.response.ResponseFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.util.List;

/**
 * Translates exceptions into the standard error envelope (spec §1.2).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log =
        LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(
        MethodArgumentNotValidException ex) {

        List<FieldError> errors = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(fe -> new FieldError(
                fe.getField(),
                fe.getDefaultMessage()))
            .toList();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ResponseFactory.error(
                ErrorCode.VALIDATION_ERROR,
                "The submitted information is invalid",
                errors));
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
        ValidationException ex) {

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ResponseFactory.error(ex));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(
        BusinessException ex) {

        HttpStatus status = mapErrorCodeToStatus(ex.getErrorCode());

        return ResponseEntity.status(status)
            .body(ResponseFactory.error(ex));
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorized(
        UnauthorizedException ex) {

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(ResponseFactory.error(ex));
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ErrorResponse> handleForbidden(
        ForbiddenException ex) {

        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(ResponseFactory.error(ex));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
        AccessDeniedException ex) {

        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(ResponseFactory.error(
                ErrorCode.FORBIDDEN,
                "Access denied"));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(
        AuthenticationException ex) {

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(ResponseFactory.error(
                ErrorCode.UNAUTHORIZED,
                "Authentication required"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(
        Exception ex,
        WebRequest request) {

        log.error(
            "Unhandled exception on {}",
            request.getDescription(false),
            ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ResponseFactory.error(
                ErrorCode.INTERNAL_ERROR,
                "An unexpected error occurred"));
    }

    private static HttpStatus mapErrorCodeToStatus(ErrorCode code) {

        return switch (code) {
            case VALIDATION_ERROR, BAD_REQUEST, MALFORMED_JSON,
                 INVALID_STATE, UNPROCESSABLE_ENTITY,
                 BUSINESS_RULE_VIOLATION ->
                HttpStatus.BAD_REQUEST;

            case UNAUTHORIZED, INVALID_CREDENTIALS,
                 TOKEN_EXPIRED, TOKEN_INVALID ->
                HttpStatus.UNAUTHORIZED;

            case FORBIDDEN, INSUFFICIENT_PERMISSIONS ->
                HttpStatus.FORBIDDEN;

            case NOT_FOUND, RESOURCE_NOT_FOUND ->
                HttpStatus.NOT_FOUND;

            case CONFLICT, DUPLICATE_RESOURCE,
                 OPTIMISTIC_LOCK_CONFLICT,
                 STATE_TRANSITION_NOT_ALLOWED ->
                HttpStatus.CONFLICT;

            case RATE_LIMIT_EXCEEDED, TOO_MANY_REQUESTS ->
                HttpStatus.TOO_MANY_REQUESTS;

            case DOWNSTREAM_SERVICE_UNAVAILABLE, SERVICE_UNAVAILABLE ->
                HttpStatus.SERVICE_UNAVAILABLE;

            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}
