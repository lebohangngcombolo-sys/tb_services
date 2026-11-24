package com.tradebridge.country.api.dto.response;

import java.time.Instant;

/**
 * Summary of a configuration version — used in the versions list.
 */
public record ConfigurationVersionResponse(
        int version,
        String status,
        String changeSummary,
        Instant publishedAt,
        Instant createdAt
) { }