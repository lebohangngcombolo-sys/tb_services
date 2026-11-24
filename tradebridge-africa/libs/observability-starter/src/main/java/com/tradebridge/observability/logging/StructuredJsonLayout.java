package com.tradebridge.observability.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import ch.qos.logback.core.LayoutBase;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tradebridge.common.util.JsonUtils;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Logback layout that emits one JSON object per log event.
 *
 * <pre>
 * {"@timestamp":"2026-11-11T10:00:00Z","level":"INFO","logger":"c.t.i.s.RegistrationService",
 *  "service":"identity-service","thread":"http-nio-8081-exec-3","correlationId":"...",
 *  "traceId":"...","spanId":"...","message":"...", "exception":"..."}
 * </pre>
 *
 * <p>All free-form fields pass through {@link SensitiveDataMasker#maskMessage(String)}
 * before serialization. MDC keys are filtered through {@link SensitiveDataMasker#maskMap}.
 */
public class StructuredJsonLayout extends LayoutBase<ILoggingEvent> {

    private static final ObjectMapper MAPPER = JsonUtils.mapper().copy();

    private final String serviceName;

    public StructuredJsonLayout() {
        this("unknown-service");
    }

    public StructuredJsonLayout(String serviceName) {
        this.serviceName = serviceName == null ? "unknown-service" : serviceName;
    }

    @Override
    public String doLayout(ILoggingEvent event) {
        ObjectNode node = MAPPER.createObjectNode();

        node.put("@timestamp", Instant.ofEpochMilli(event.getTimeStamp()).toString());
        node.put("level", event.getLevel().toString());
        node.put("logger", event.getLoggerName());
        node.put("service", serviceName);
        node.put("thread", event.getThreadName());

        // MDC — masked, sensitive keys dropped
        Map<String, String> mdc = event.getMDCPropertyMap();
        if (mdc != null && !mdc.isEmpty()) {
            Map<String, Object> filtered = new LinkedHashMap<>();
            for (Map.Entry<String, String> e : mdc.entrySet()) {
                if (!SensitiveDataMasker.isDropKey(e.getKey())) {
                    filtered.put(e.getKey(), e.getValue());
                }
            }
            Map<String, Object> masked = SensitiveDataMasker.maskMap(filtered);
            masked.forEach((k, v) -> node.put(k, v == null ? null : v.toString()));
        }

        // Message — always masked
        node.put("message", SensitiveDataMasker.maskMessage(event.getFormattedMessage()));

        // Exception — attach throwable chain as structured string
        IThrowableProxy tp = event.getThrowableProxy();
        if (tp != null) {
            String raw = ThrowableProxyUtil.asString(tp);
            node.put("exception", SensitiveDataMasker.maskMessage(raw));
        }

        // Trailing newline is required for line-based ingestion (Loki, ELK)
        return node.toString() + System.lineSeparator();
    }
}
