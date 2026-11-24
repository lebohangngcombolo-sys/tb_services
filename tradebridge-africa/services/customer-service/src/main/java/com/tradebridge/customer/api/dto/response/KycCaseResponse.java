package com.tradebridge.customer.api.dto.response;

import java.time.Instant;
import java.util.UUID;

public record KycCaseResponse(
        UUID id,
        UUID organizationId,
        String caseNumber,
        String caseType,
        String countryCode,
        int countryConfigurationVersion,
        String status,
        String riskLevel,
        UUID assignedReviewerId,
        Instant submittedAt,
        Instant completedAt,
        Instant createdAt,
        Instant updatedAt
) { }