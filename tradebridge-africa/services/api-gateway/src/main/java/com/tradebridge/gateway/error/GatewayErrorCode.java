package com.tradebridge.gateway.error;

/**
 * Gateway-specific error codes surfaced to clients. Where a matching
 * {@link com.tradebridge.common.exception.ErrorCode} exists, prefer that —
 * these codes exist only for gateway-level concerns.
 */
public enum GatewayErrorCode {
    UNAUTHORIZED,
    FORBIDDEN,
    ROUTE_NOT_FOUND,
    DOWNSTREAM_UNAVAILABLE,
    REQUEST_TOO_LARGE,
    RATE_LIMIT_EXCEEDED,
    METHOD_NOT_ALLOWED,
    MALFORMED_REQUEST
}