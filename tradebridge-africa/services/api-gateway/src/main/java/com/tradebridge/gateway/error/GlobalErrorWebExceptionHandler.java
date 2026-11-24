package com.tradebridge.gateway.error;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradebridge.common.exception.ErrorCode;
import com.tradebridge.common.response.ResponseFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Reactive counterpart to {@code @RestControllerAdvice} — catches unhandled
 * exceptions and writes the standard error envelope (spec §1.2).
 *
 * <p>Kept narrow: the security chain, rate limiter, request-size filter,
 * and downstream filter each write their own error envelope before this
 * handler would be triggered. This handler is the last line of defense.
 */
@Component
@Order(-1)
public class GlobalErrorWebExceptionHandler implements ErrorWebExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalErrorWebExceptionHandler.class);

    private final ObjectMapper objectMapper;

    public GlobalErrorWebExceptionHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        if (exchange.getResponse().isCommitted()) {
            return Mono.error(ex);
        }
        ServerHttpResponse response = exchange.getResponse();
        HttpStatus status;
        ErrorCode code;

        if (ex instanceof ResponseStatusException rse) {
            status = HttpStatus.valueOf(rse.getStatusCode().value());
            code = switch (status.value()) {
                case 400 -> ErrorCode.BAD_REQUEST;
                case 401 -> ErrorCode.UNAUTHORIZED;
                case 403 -> ErrorCode.FORBIDDEN;
                case 404 -> ErrorCode.NOT_FOUND;
                case 405 -> ErrorCode.BAD_REQUEST;
                case 429 -> ErrorCode.RATE_LIMIT_EXCEEDED;
                case 503 -> ErrorCode.SERVICE_UNAVAILABLE;
                default -> ErrorCode.INTERNAL_ERROR;
            };
        } else {
            log.warn("Unhandled gateway exception on {}",
                    exchange.getRequest().getURI().getPath(), ex);
            status = HttpStatus.INTERNAL_SERVER_ERROR;
            code = ErrorCode.INTERNAL_ERROR;
        }

        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        try {
            byte[] bytes = objectMapper.writeValueAsBytes(
                    ResponseFactory.error(code, "Request failed"));
            DataBuffer buffer = response.bufferFactory().wrap(bytes);
            return response.writeWith(Mono.just(buffer));
        } catch (Exception e) {
            return response.setComplete();
        }
    }
}