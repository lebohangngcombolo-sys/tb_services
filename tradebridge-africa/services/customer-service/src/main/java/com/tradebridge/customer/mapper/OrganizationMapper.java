package com.tradebridge.customer.mapper;

import com.tradebridge.customer.api.dto.response.OrganizationResponse;
import com.tradebridge.customer.api.dto.response.OrganizationStatusResponse;
import com.tradebridge.customer.domain.KycCase;
import com.tradebridge.customer.domain.Organization;
import org.springframework.stereotype.Component;

/**
 * Mapper for organization views.
 */
@Component
public class OrganizationMapper {

    public OrganizationResponse toResponse(Organization org) {
        if (org == null) return null;
        return new OrganizationResponse(
                org.getId(),
                org.getOrganizationNumber(),
                org.getLegalName(),
                org.getTradingName(),
                org.getOrganizationType(),
                org.getRegistrationNumber(),
                org.getTaxNumber(),
                org.getCountryCode(),
                org.getIncorporationDate(),
                org.getRegistrationAuthority(),
                org.getBusinessDescription(),
                org.getWebsite(),
                org.getStatus().name(),
                org.getCreatedByUserId(),
                org.getCreatedAt(),
                org.getUpdatedAt());
    }

    public OrganizationStatusResponse toStatusResponse(Organization org, KycCase activeCase) {
        if (org == null) return null;
        return new OrganizationStatusResponse(
                org.getId(),
                org.getStatus().name(),
                activeCase == null ? null : activeCase.getId(),
                activeCase == null ? null : activeCase.getStatus().name(),
                org.getUpdatedAt());
    }
}