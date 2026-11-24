package com.tradebridge.customer.domain.enums;

/**
 * KYC document state (spec §6.2 {@code kyc_documents.document_status}).
 *
 * <p>Documents move from PENDING → SCANNING (async) → ACCEPTED or
 * REJECTED. Expired documents are flagged with EXPIRED without changing
 * the ACCEPTED state (kept for audit trail).
 */
public enum DocumentStatus {
    PENDING,
    SCANNING,
    ACCEPTED,
    REJECTED,
    EXPIRED
}