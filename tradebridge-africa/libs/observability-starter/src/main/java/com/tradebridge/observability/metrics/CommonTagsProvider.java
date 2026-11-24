package com.tradebridge.observability.metrics;

import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;

import java.util.Objects;

/**
 * Supplies the {@code service} tag applied to every metric via a
 * {@code MeterRegistryCustomizer}. Additional global tags can be configured.
 */
public class CommonTagsProvider {

    private final String serviceName;
    private final String environment;
    private final String region;

    public CommonTagsProvider(String serviceName, String environment, String region) {
        this.serviceName = Objects.requireNonNull(serviceName, "serviceName");
        this.environment = environment;
        this.region = region;
    }

    public Tags commonTags() {
        Tags tags = Tags.of(Tag.of(MetricNames.TAG_SERVICE, serviceName));
        if (environment != null && !environment.isBlank()) {
            tags = tags.and("env", environment);
        }
        if (region != null && !region.isBlank()) {
            tags = tags.and("region", region);
        }
        return tags;
    }
}
