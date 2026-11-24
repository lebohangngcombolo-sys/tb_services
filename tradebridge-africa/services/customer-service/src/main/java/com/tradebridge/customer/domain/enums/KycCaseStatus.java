package com.tradebridge.customer.domain.enums;

/**
 * KYC case lifecycle (spec §6.2 {@code kyc_cases.status}).
 *
 * <p>State transitions:
 * <pre>
 *   DRAFT                     → SUBMITTED
 *   SUBMITTED                 → IN_REVIEW, INFORMATION_REQUESTED
 *   IN_REVIEW                 → APPROVED, REJECTED, INFORMATION_REQUESTED
 *   INFORMATION_REQUESTED     → RESUBMITTED
 *   RESUBMITTED               → IN_REVIEW, APPROVED, REJECTED
 *   APPROVED / REJECTED       (terminal — new case required to reattempt)
 * </pre>
 */
public enum KycCaseStatus {
    DRAFT,
    SUBMITTED,
    IN_REVIEW,
    INFORMATION_REQUESTED,
    RESUBMITTED,
    APPROVED,
    REJECTED;

    public boolean isTerminal() {
        return this == APPROVED || this == REJECTED;
    }

    public boolean requiresAssignee() {
        return this == IN_REVIEW || this == INFORMATION_REQUESTED;
    }

    public boolean canAcceptDecision() {
        return this == IN_REVIEW || this == RESUBMITTED;
    }
}