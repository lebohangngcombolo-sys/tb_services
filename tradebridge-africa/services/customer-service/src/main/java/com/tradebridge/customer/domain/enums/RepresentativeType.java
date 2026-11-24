package com.tradebridge.customer.domain.enums;

/**
 * Representative role within an organization (spec §6.2
 * {@code organization_representatives.representative_type}).
 */
public enum RepresentativeType {
    DIRECTOR,
    OFFICER,
    SHAREHOLDER,
    SIGNATORY,
    OTHER
}