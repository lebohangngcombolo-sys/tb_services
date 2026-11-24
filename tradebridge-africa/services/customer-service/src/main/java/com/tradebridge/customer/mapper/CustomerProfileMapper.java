package com.tradebridge.customer.mapper;

import com.tradebridge.customer.api.dto.response.CustomerProfileResponse;
import com.tradebridge.customer.domain.CustomerProfile;
import org.springframework.stereotype.Component;

/**
 * Stateless mapper for customer profiles.
 */
@Component
public class CustomerProfileMapper {

    public CustomerProfileResponse toResponse(CustomerProfile profile) {
        if (profile == null) return null;
        return new CustomerProfileResponse(
                profile.getId(),
                profile.getIdentityUserId(),
                profile.getCustomerNumber(),
                profile.getCustomerType(),
                profile.getCountryCode(),
                profile.getPreferredLanguage(),
                profile.getStatus().name(),
                profile.getCreatedAt(),
                profile.getUpdatedAt());
    }
}