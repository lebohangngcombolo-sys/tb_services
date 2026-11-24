package com.tradebridge.gateway.telemetry;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Operational metrics specific to the Gateway (spec §2.4 "These belong in
 * logs, metrics and traces, not in the business Kafka event stream").
 *
 * <p>Counters:
 * <ul>
 *   <li>{@code tb.gateway.requests.total} — per route and status</li>
 *   <li>{@code tb.gateway.rejections.total} — per reason</li>
 *   <li>{@code tb.gateway.rate_limit.exceeded.total} — per bucket</li>
 * </ul>
 *
 * <p>Timer:
 * <ul>
 *   <li>{@code tb.gateway.request.duration} — per route</li>
 * </ul>
 */
@Component
public class GatewayTelemetry {

    private final MeterRegistry meterRegistry;
    private final Map<String, Counter> requestCounters = new ConcurrentHashMap<>();
    private final Map<String, Counter> rejectionCounters = new ConcurrentHashMap<>();
    private final Map<String, Timer> routeTimers = new ConcurrentHashMap<>();

    public GatewayTelemetry(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void recordRequest(ServerWebExchange exchange, int status) {
        String route = extractRoute(exchange);
        Counter counter = requestCounters.computeIfAbsent(route + "|" + status, k ->
                Counter.builder("tb.gateway.requests.total")
                        .tag("route", route)
                        .tag("status", String.valueOf(status))
                        .register(meterRegistry));
        counter.increment();
    }

    public void recordRejection(ServerWebExchange exchange, String reason) {
        String route = extractRoute(exchange);
        Counter counter = rejectionCounters.computeIfAbsent(route + "|" + reason, k ->
                Counter.builder("tb.gateway.rejections.total")
                        .tag("route", route)
                        .tag("reason", reason)
                        .register(meterRegistry));
        counter.increment();
    }

    public Timer.Sample startTimer() {
        return Timer.start(meterRegistry);
    }

    public void stopTimer(Timer.Sample sample, ServerWebExchange exchange) {
        String route = extractRoute(exchange);
        Timer timer = routeTimers.computeIfAbsent(route, k ->
                Timer.builder("tb.gateway.request.duration")
                        .tag("route", route)
                        .register(meterRegistry));
        sample.stop(timer);
    }

    private static String extractRoute(ServerWebExchange exchange) {
        // Derive a stable route label from the first path segment after /api/v1/
        String path = exchange.getRequest().getURI().getPath();
        String[] parts = path.split("/");
        for (int i = 0; i < parts.length; i++) {
            if ("v1".equals(parts[i]) && i + 1 < parts.length) {
                return parts[i + 1];
            }
        }
        return path.isEmpty() ? "root" : parts.length > 1 ? parts[1] : path;
    }
}