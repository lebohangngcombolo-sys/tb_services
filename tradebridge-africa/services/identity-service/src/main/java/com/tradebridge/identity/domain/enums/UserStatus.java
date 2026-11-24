package com.tradebridge.identity.domain.enums;

/**
 * User account lifecycle status (spec §3.2 "Suggested statuses").
 *
 * <p>State transitions:
 * <pre>
 *   PENDING_VERIFICATION → ACTIVE
 *   ACTIVE               → SUSPENDED, LOCKED, CLOSED
 *   SUSPENDED            → ACTIVE (reactivate), CLOSED
 *   LOCKED               → ACTIVE (unlock)
 *   CLOSED               → (terminal)
 * </pre>
 */
public enum UserStatus {
    PENDING_VERIFICATION,
    ACTIVE,
    SUSPENDED,
    LOCKED,
    CLOSED;

    public boolean canAuthenticate() {
        return this == ACTIVE;
    }

    public boolean isTerminal() {
        return this == CLOSED;
    }
}