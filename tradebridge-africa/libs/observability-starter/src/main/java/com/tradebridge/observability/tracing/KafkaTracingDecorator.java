package com.tradebridge.observability.tracing;

import com.tradebridge.observability.logging.MdcKeys;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.slf4j.MDC;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Handles W3C trace-context injection/extraction on Kafka records.
 *
 * <p>Producers: attach current trace/span IDs as headers before sending.
 * Consumers: restore trace/span IDs as MDC entries for the duration of
 * the handler, then close the span.
 *
 * <p>Separate from the auto-configured {@code PropagatingSenderTracingObservationHandler}
 * so that services using the messaging-starter's {@code KafkaEventPublisher}
 * can add decoration explicitly if the auto-handler isn't triggered.
 */
public class KafkaTracingDecorator {

    public static final String HEADER_TRACE_ID = "traceparent";

    private final Tracer tracer;

    public KafkaTracingDecorator(Tracer tracer) {
        this.tracer = Objects.requireNonNull(tracer, "tracer");
    }

    /** Adds {@code traceparent} header to an outbound record. */
    public void inject(ProducerRecord<?, ?> record) {
        if (record == null) return;
        Span current = tracer.currentSpan();
        if (current == null) return;
        TraceContext ctx = current.context();
        String traceparent = "00-" + ctx.traceId() + "-" + ctx.spanId() + "-" + sampled(ctx) ;
        record.headers().add(HEADER_TRACE_ID, traceparent.getBytes(StandardCharsets.UTF_8));
    }

    /** Extracts trace context from an inbound record and returns a scope-closeable handle. */
    public Consumer<TraceContext> extract(ConsumerRecord<?, ?> record) {
        if (record == null) return ctx -> { };
        String traceparent = header(record.headers(), HEADER_TRACE_ID);
        if (traceparent == null) {
            return ctx -> { };
        }
        String[] parts = traceparent.split("-");
        if (parts.length < 4) {
            return ctx -> { };
        }
        String traceId = parts[1];
        String spanId = parts[2];
        MDC.put(MdcKeys.TRACE_ID, traceId);
        MDC.put(MdcKeys.SPAN_ID, spanId);
        return ctx -> {
            MDC.remove(MdcKeys.TRACE_ID);
            MDC.remove(MdcKeys.SPAN_ID);
        };
    }

    private static String sampled(TraceContext ctx) {
        return "01"; // we don't currently expose sampling decisions; default to sampled
    }

    private static String header(Headers headers, String name) {
        if (headers == null) return null;
        for (Header h : headers.headers(name)) {
            return new String(h.value(), StandardCharsets.UTF_8);
        }
        return null;
    }
}
