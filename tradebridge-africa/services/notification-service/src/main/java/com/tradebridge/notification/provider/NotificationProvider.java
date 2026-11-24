package com.tradebridge.notification.provider;

import com.tradebridge.common.enums.Channel;

/**
 * Provider contract (spec §5.5). Providers are pluggable, replaceable
 * vendor adapters — business services never call SMTP or SMS vendor APIs
 * directly.
 */
public interface NotificationProvider {

    /** Provider name that appears in {@code notification_deliveries.provider}. */
    String name();

    /** Channel this provider handles. */
    Channel channel();

    /** True if the provider is configured and enabled. */
    boolean isEnabled();

    /**
     * Deliver a message.
     *
     * @param destination raw destination (email or E.164 phone number)
     * @param subject     rendered subject (nullable for SMS/IN_APP)
     * @param body        rendered body (never logged)
     * @return delivery outcome; on success must return {@link ProviderResult#accepted}
     */
    ProviderResult send(String destination, String subject, String body);

    /** Lightweight health check for the {@code /admin/health} endpoint. */
    default boolean healthCheck() {
        return isEnabled();
    }
}