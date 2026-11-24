package com.tradebridge.customer.event.payload;

import java.util.UUID;

/**
 * {@code customer.kyc.review.started.v1} payload (spec §6.3).
 */
public record KycReviewStartedPayload(
        UUID kycCaseId,
        UUID reviewerId
) { }