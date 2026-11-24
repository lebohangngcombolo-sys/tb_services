package com.tradebridge.common.enums;

/**
 * Notification priority (spec §5.1A / §5.2).
 * CRITICAL covers security-critical messages that bypass user preferences (§5.4).
 */
public enum Priority {
    LOW,
    NORMAL,
    HIGH,
    CRITICAL
}