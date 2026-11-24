package com.tradebridge.customer.mapper;

import com.tradebridge.customer.api.dto.response.RepresentativeResponse;
import com.tradebridge.customer.domain.OrganizationRepresentative;
import org.springframework.stereotype.Component;

@Component
public class RepresentativeMapper {

    public RepresentativeResponse toResponse(OrganizationRepresentative rep) {
        if (rep == null) return null;
        return new RepresentativeResponse(
                rep.getId(),
                rep.getOrganizationId(),
                rep.getIdentityUserId(),
                rep.getFirstName(),
                rep.getLastName(),
                rep.getEmail(),
                rep.getPhoneNumber(),
                rep.getRepresentativeType().name(),
                rep.isAuthorizedSignatory(),
                rep.getVerificationStatus().name(),
                rep.getCreatedAt());
    }
}