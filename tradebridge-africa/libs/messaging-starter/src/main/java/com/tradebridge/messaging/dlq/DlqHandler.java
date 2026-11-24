package com.tradebridge.messaging.dlq;

import com.tradebridge.messaging.event.EventEnvelope;
import org.apache.kafka.clients.consumer.ConsumerRecord;

/**
 * Optional hook services can implement to be notified when one of their
 * messages lands in a DLQ (e.g. alerting, metrics). The router calls this
 * after routing.
 */
public interface DlqHandler {

    void onRouted(ConsumerRecord<String, EventEnvelope> original,
                  String consumerName,
                  String reason);
}
