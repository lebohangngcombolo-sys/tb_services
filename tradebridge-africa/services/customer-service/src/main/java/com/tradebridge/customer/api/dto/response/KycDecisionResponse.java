package com.tradebridge.customer.api.dto.response;

import java.time.Instant;
import java.util.UUID;

public record KycDecisionResponse(
        UUID id,
        UUID kycCaseId,
        String decision,
        String decisionReasonCode,
        String decisionReason,
        UUID decidedByUserId,
        int decisionVersion,
        UUID supersedesDecisionId,
        Instant decidedAt
) { }