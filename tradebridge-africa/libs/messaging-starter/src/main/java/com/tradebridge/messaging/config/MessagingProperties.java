package com.tradebridge.messaging.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Central configuration for the messaging starter.
 *
 * <pre>
 * tradebridge.messaging:
 *   producer:
 *     enabled: true
 *     default-topic: ""
 *     acks: all
 *     retries: 3
 *     enable-idempotence: true
 *   consumer:
 *     enabled: true
 *     group-id-prefix: ""
 *     auto-offset-reset: earliest
 *     max-poll-records: 100
 *   outbox:
 *     enabled: true
 *     poll-interval: 5s
 *     batch-size: 100
 *     max-attempts: 5
 *     topic-per-service: ""       # e.g. "identity.events" — publisher's single topic
 *   inbox:
 *     enabled: true
 *     retention: 30d
 *   retry:
 *     max-attempts: 5
 *     initial-backoff: 1s
 *     max-backoff: 30s
 *     multiplier: 2.0
 *   dlq:
 *     enabled: true
 *     suffix: ".dlq"
 *   topics:
 *     - name: identity.events
 *       partitions: 3
 *       replication-factor: 1
 *     - name: identity.events.dlq
 *       partitions: 3
 *       replication-factor: 1
 * </pre>
 */
@ConfigurationProperties(prefix = "tradebridge.messaging")
public class MessagingProperties {

    private final Producer producer = new Producer();
    private final Consumer consumer = new Consumer();
    private final Outbox outbox = new Outbox();
    private final Inbox inbox = new Inbox();
    private final Retry retry = new Retry();
    private final Dlq dlq = new Dlq();
    private final List<Topic> topics = new ArrayList<>();

    public Producer getProducer() { return producer; }
    public Consumer getConsumer() { return consumer; }
    public Outbox getOutbox() { return outbox; }
    public Inbox getInbox() { return inbox; }
    public Retry getRetry() { return retry; }
    public Dlq getDlq() { return dlq; }
    public List<Topic> getTopics() { return topics; }

    public static class Producer {
        private boolean enabled = true;
        private String defaultTopic;
        private String acks = "all";
        private int retries = 3;
        private boolean enableIdempotence = true;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean v) { this.enabled = v; }
        public String getDefaultTopic() { return defaultTopic; }
        public void setDefaultTopic(String v) { this.defaultTopic = v; }
        public String getAcks() { return acks; }
        public void setAcks(String v) { this.acks = v; }
        public int getRetries() { return retries; }
        public void setRetries(int v) { this.retries = v; }
        public boolean isEnableIdempotence() { return enableIdempotence; }
        public void setEnableIdempotence(boolean v) { this.enableIdempotence = v; }
    }

    public static class Consumer {
        private boolean enabled = true;
        private String groupIdPrefix = "";
        private String autoOffsetReset = "earliest";
        private int maxPollRecords = 100;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean v) { this.enabled = v; }
        public String getGroupIdPrefix() { return groupIdPrefix; }
        public void setGroupIdPrefix(String v) { this.groupIdPrefix = v; }
        public String getAutoOffsetReset() { return autoOffsetReset; }
        public void setAutoOffsetReset(String v) { this.autoOffsetReset = v; }
        public int getMaxPollRecords() { return maxPollRecords; }
        public void setMaxPollRecords(int v) { this.maxPollRecords = v; }
    }

    public static class Outbox {
        private boolean enabled = true;
        private Duration pollInterval = Duration.ofSeconds(5);
        private int batchSize = 100;
        private int maxAttempts = 5;
        private String topicPerService = "";

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean v) { this.enabled = v; }
        public Duration getPollInterval() { return pollInterval; }
        public void setPollInterval(Duration d) { this.pollInterval = d; }
        public int getBatchSize() { return batchSize; }
        public void setBatchSize(int v) { this.batchSize = v; }
        public int getMaxAttempts() { return maxAttempts; }
        public void setMaxAttempts(int v) { this.maxAttempts = v; }
        public String getTopicPerService() { return topicPerService; }
        public void setTopicPerService(String v) { this.topicPerService = v; }
    }

    public static class Inbox {
        private boolean enabled = true;
        private Duration retention = Duration.ofDays(30);

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean v) { this.enabled = v; }
        public Duration getRetention() { return retention; }
        public void setRetention(Duration d) { this.retention = d; }
    }

    public static class Retry {
        private int maxAttempts = 5;
        private Duration initialBackoff = Duration.ofSeconds(1);
        private Duration maxBackoff = Duration.ofSeconds(30);
        private double multiplier = 2.0;

        public int getMaxAttempts() { return maxAttempts; }
        public void setMaxAttempts(int v) { this.maxAttempts = v; }
        public Duration getInitialBackoff() { return initialBackoff; }
        public void setInitialBackoff(Duration d) { this.initialBackoff = d; }
        public Duration getMaxBackoff() { return maxBackoff; }
        public void setMaxBackoff(Duration d) { this.maxBackoff = d; }
        public double getMultiplier() { return multiplier; }
        public void setMultiplier(double v) { this.multiplier = v; }
    }

    public static class Dlq {
        private boolean enabled = true;
        private String suffix = ".dlq";

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean v) { this.enabled = v; }
        public String getSuffix() { return suffix; }
        public void setSuffix(String v) { this.suffix = v; }
    }

    public static class Topic {
        private String name;
        private int partitions = 3;
        private int replicationFactor = 1;

        public String getName() { return name; }
        public void setName(String v) { this.name = v; }
        public int getPartitions() { return partitions; }
        public void setPartitions(int v) { this.partitions = v; }
        public int getReplicationFactor() { return replicationFactor; }
        public void setReplicationFactor(int v) { this.replicationFactor = v; }
    }
}
