package com.tradebridge.customer.domain.enums;

/**
 * Nature of beneficial ownership / control (spec §6.2
 * {@code beneficial_owners.control_type}).
 *
 * <p>DIRECT and INDIRECT carry an ownership_percentage. CONTROL and
 * TRUST do not require a percentage (ownership may be null). NOMINEE
 * covers arrangements where a person holds shares on behalf of another.
 */
public enum ControlType {
    DIRECT,
    INDIRECT,
    CONTROL,
    TRUST,
    NOMINEE;

    public boolean requiresOwnershipPercentage() {
        return this == DIRECT || this == INDIRECT;
    }
}