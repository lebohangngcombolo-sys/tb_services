package com.tradebridge.common.exception;

/**
 * Raised when the caller's request conflicts with current state (duplicate,
 * optimistic lock, or disallowed state transition).
 */
public class ConflictException extends BusinessException {

    public ConflictException(String message) {
        super(ErrorCode.CONFLICT, message);
    }

    public ConflictException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}