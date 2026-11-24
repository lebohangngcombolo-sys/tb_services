package com.tradebridge.customer.api.dto.response;

import java.time.Instant;
import java.util.UUID;

public record KycReviewResponse(
        UUID id,
        UUID kycCaseId,
        UUID reviewerUserId,
        String reviewStage,
        String reviewStatus,
        String reviewNotes,
        Instant startedAt,
        Instant completedAt,
        Instant createdAt
) { }