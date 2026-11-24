package com.tradebridge.customer.client.dto;

import java.time.Instant;
import java.util.Map;

/**
 * Response of {@code GET /api/v1/countries/{code}/configuration} — the
 * active configuration and its version. Spec §11 decision 3 requires
 * recording the version at KYC submission.
 */
public record ActiveConfigurationResponse(
        String countryCode,
        int version,
        String status,
        String changeSummary,
        Map<String, Object> configuration,
        Instant publishedAt,
        Instant createdAt
) { }