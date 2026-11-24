
package com.tradebridge.identity.mapper;

import com.tradebridge.identity.api.dto.response.UserProfileResponse;
import com.tradebridge.identity.domain.User;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Manual mapper — kept simple and side-effect-free so it can be used from
 * read-only transactional methods without triggering lazy loads it does not
 * already handle.
 */
@Component
public final class UserMapper {

    public UserProfileResponse toProfile(User user, List<String> roles) {
        if (user == null) {
            return null;
        }

        return new UserProfileResponse(
            user.getId(),
            user.getFirstName(),
            user.getLastName(),
            user.getEmail(),
            user.getPhoneNumber(),
            user.getCountryCode(),
            user.getPreferredLanguage(),
            user.getStatus() == null ? null : user.getStatus().name(),
            user.isEmailVerified(),
            user.isPhoneVerified(),
            user.isMfaEnabled(),
            roles == null ? List.of() : List.copyOf(roles),
            user.getCreatedAt(),
            user.getUpdatedAt()
        );
    }
}
