package com.tradebridge.customer.domain.enums;

/**
 * Verification status shared across representatives, beneficial owners,
 * and related sub-entities of an organization (spec §6.2
 * {@code organization_representatives.verification_status} and
 * {@code beneficial_owners.verification_status}).
 */
public enum RepresentativeVerificationStatus {
    PENDING,
    VERIFIED,
    REJECTED
}
