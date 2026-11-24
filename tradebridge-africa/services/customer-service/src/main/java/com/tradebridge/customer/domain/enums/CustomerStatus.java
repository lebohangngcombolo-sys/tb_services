package com.tradebridge.customer.domain.enums;

/**
 * Customer profile lifecycle (spec §6.2 {@code customer_profiles.status}).
 *
 * <p>Customers can be ACTIVE while their organization's KYC is still
 * PENDING_REVIEW — profile status is independent of organization status.
 */
public enum CustomerStatus {
    PENDING,
    ACTIVE,
    SUSPENDED,
    CLOSED;

    public boolean isTerminal() {
        return this == CLOSED;
    }
}