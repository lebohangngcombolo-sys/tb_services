package com.tradebridge.gateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradebridge.common.exception.ErrorCode;
import com.tradebridge.common.response.ResponseFactory;
import com.tradebridge.gateway.telemetry.RejectionLogger;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.ConnectException;

/**
 * Transforms downstream connectivity failures into the standard error
 * envelope (spec §2.4 "Downstream service unavailable").
 *
 * <p>Handles both:
 * <ul>
 *   <li>Connection refused (service down)</li>
 *   <li>Response timeout (service slow)</li>
 * </ul>
 * Both become HTTP 503 with the standard body, so clients see a
 * consistent error shape.
 */
@Component
public class DownstreamUnavailableFilter implements GlobalFilter, Ordered {

    private final RejectionLogger rejectionLogger;
    private final ObjectMapper objectMapper;

    public DownstreamUnavailableFilter(RejectionLogger rejectionLogger, ObjectMapper objectMapper) {
        this.rejectionLogger = rejectionLogger;
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return chain.filter(exchange)
                .onErrorResume(ex -> {
                    Throwable cause = unwrap(ex);
                    if (cause instanceof ConnectException
                            || cause instanceof java.util.concurrent.TimeoutException
                            || cause instanceof io.netty.handler.timeout.ReadTimeoutException
                            || cause instanceof io.netty.handler.timeout.TimeoutException) {
                        rejectionLogger.logRejection(exchange, "DOWNSTREAM_UNAVAILABLE",
                                exchange.getRequest().getURI().getPath());
                        return writeError(exchange, objectMapper);
                    }
                    return Mono.error(ex);
                });
    }

    private static Throwable unwrap(Throwable ex) {
        Throwable current = ex;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current;
    }

    private Mono<Void> writeError(ServerWebExchange exchange, ObjectMapper mapper) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        try {
            byte[] bytes = mapper.writeValueAsBytes(
                    ResponseFactory.error(ErrorCode.DOWNSTREAM_SERVICE_UNAVAILABLE,
                            "A required downstream service is currently unavailable"));
            DataBuffer buffer = response.bufferFactory().wrap(bytes);
            return response.writeWith(Mono.just(buffer));
        } catch (Exception e) {
            return response.setComplete();
        }
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE - 500;
    }
}