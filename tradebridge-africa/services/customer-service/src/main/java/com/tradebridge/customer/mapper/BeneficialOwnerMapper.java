package com.tradebridge.customer.mapper;

import com.tradebridge.customer.api.dto.response.BeneficialOwnerResponse;
import com.tradebridge.customer.domain.BeneficialOwner;
import org.springframework.stereotype.Component;

/**
 * Mapper for beneficial owners.
 *
 * <p>Does NOT expose {@code identityReferenceHash} — that field is
 * internal-only and represents sensitive identity data (spec §6.4
 * "Sensitive personal data must have restricted access").
 */
@Component
public class BeneficialOwnerMapper {

    public BeneficialOwnerResponse toResponse(BeneficialOwner owner) {
        if (owner == null) return null;
        return new BeneficialOwnerResponse(
                owner.getId(),
                owner.getOrganizationId(),
                owner.getFirstName(),
                owner.getLastName(),
                owner.getDateOfBirth(),
                owner.getNationalityCountryCode(),
                owner.getResidenceCountryCode(),
                owner.getOwnershipPercentage(),
                owner.getControlType().name(),
                owner.getVerificationStatus().name(),
                owner.getCreatedAt(),
                owner.getUpdatedAt());
    }
}