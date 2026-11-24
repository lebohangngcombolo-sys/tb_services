package com.tradebridge.customer.domain.enums;

/**
 * Organization lifecycle (spec §6.2 {@code organizations.status}).
 *
 * <p>State transitions:
 * <pre>
 *   DRAFT           → PENDING_REVIEW  (submit)
 *                   → WITHDRAWN       (withdraw without submission)
 *   PENDING_REVIEW  → APPROVED        (KYC approved)
 *                   → REJECTED        (KYC rejected)
 *                   → WITHDRAWN       (withdraw while pending)
 *   APPROVED        → (terminal — subsequent KYC is a new case)
 *   REJECTED        → PENDING_REVIEW  (resubmit with new case)
 *   WITHDRAWN       → PENDING_REVIEW  (resubmit)
 * </pre>
 */
public enum OrganizationStatus {
    DRAFT,
    PENDING_REVIEW,
    APPROVED,
    REJECTED,
    WITHDRAWN;

    public boolean canBeEdited() {
        return this == DRAFT || this == WITHDRAWN;
    }

    public boolean canBeSubmitted() {
        return this == DRAFT || this == REJECTED || this == WITHDRAWN;
    }
}