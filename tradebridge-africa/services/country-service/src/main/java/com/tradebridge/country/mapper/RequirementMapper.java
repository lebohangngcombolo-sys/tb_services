package com.tradebridge.country.mapper;

import com.tradebridge.country.api.dto.response.KycRequirementResponse;
import com.tradebridge.country.api.dto.response.OrganizationRequirementResponse;
import com.tradebridge.country.domain.KycRequirement;
import com.tradebridge.country.domain.OrganizationRequirement;

public final class RequirementMapper {

    public OrganizationRequirementResponse toResponse(OrganizationRequirement r) {
        if (r == null) return null;
        return new OrganizationRequirementResponse(
                r.getId(),
                r.getCountryCode(),
                r.getOrganizationType(),
                r.getRequirementCode(),
                r.getDescription(),
                r.isRequired(),
                r.getValidationRule(),
                r.getEffectiveFrom(),
                r.getEffectiveUntil(),
                r.isEnabled());
    }

    public KycRequirementResponse toResponse(KycRequirement r) {
        if (r == null) return null;
        return new KycRequirementResponse(
                r.getId(),
                r.getCountryCode(),
                r.getCustomerType(),
                r.getDocumentType(),
                r.getRequirementCode(),
                r.getDescription(),
                r.isRequired(),
                r.getAllowedMimeTypes(),
                r.getMaxFileSizeBytes(),
                r.getValidityDays(),
                r.isEnabled());
    }
}