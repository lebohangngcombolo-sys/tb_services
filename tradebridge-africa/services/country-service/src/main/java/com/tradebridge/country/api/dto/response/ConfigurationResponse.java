package com.tradebridge.country.api.dto.response;

import java.time.Instant;
import java.util.Map;

/**
 * A country's active (or specific) configuration version.
 */
public record ConfigurationResponse(
        String countryCode,
        int version,
        String status,
        String changeSummary,
        Map<String, Object> configuration,
        Instant publishedAt,
        Instant createdAt
) { }