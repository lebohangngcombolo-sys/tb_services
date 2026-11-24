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

/**
 * Rejects requests larger than a configured threshold (spec §2.5
 * "Enforce request size limits").
 *
 * <p>Threshold is generous (25 MB) because KYC document uploads go
 * through the Gateway; the actual file size is validated by the
 * customer-service. This is a defense-in-depth cap, not the business limit.
 */
@Component
public class RequestSizeFilter implements GlobalFilter, Ordered {

    private static final long MAX_CONTENT_LENGTH = 25L * 1024 * 1024;

    private final RejectionLogger rejectionLogger;
    private final ObjectMapper objectMapper;

    public RequestSizeFilter(RejectionLogger rejectionLogger, ObjectMapper objectMapper) {
        this.rejectionLogger = rejectionLogger;
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        Long contentLength = exchange.getRequest().getHeaders().getContentLength();
        if (contentLength > MAX_CONTENT_LENGTH) {
            rejectionLogger.logRejection(exchange, "REQUEST_TOO_LARGE",
                    exchange.getRequest().getURI().getPath());
            ServerHttpResponse response = exchange.getResponse();
            response.setStatusCode(HttpStatus.PAYLOAD_TOO_LARGE);
            response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
            try {
                byte[] bytes = objectMapper.writeValueAsBytes(
                        ResponseFactory.error(ErrorCode.VALIDATION_ERROR,
                                "Request exceeds maximum allowed size"));
                DataBuffer buffer = response.bufferFactory().wrap(bytes);
                return response.writeWith(Mono.just(buffer));
            } catch (Exception e) {
                return response.setComplete();
            }
        }
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 50;
    }
}