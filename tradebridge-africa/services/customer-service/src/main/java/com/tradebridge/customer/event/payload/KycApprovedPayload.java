package com.tradebridge.customer.event.payload;

import java.util.UUID;

/**
 * {@code customer.kyc.approved.v1} payload (spec §6.3).
 */
public record KycApprovedPayload(
        UUID organizationId,
        UUID kycCaseId,
        UUID decisionId
) { }