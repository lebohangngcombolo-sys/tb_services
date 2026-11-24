package com.tradebridge.customer.event.payload;

import java.util.UUID;

/**
 * {@code customer.kyc.decision.reversed.v1} payload (spec §6.3).
 * Fired when a decision is superseded by a new one.
 */
public record KycDecisionReversedPayload(
        UUID kycCaseId,
        UUID previousDecisionId,
        UUID newDecisionId
) { }