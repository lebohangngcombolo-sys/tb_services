package com.tradebridge.identity.security;

import com.tradebridge.identity.domain.Role;
import com.tradebridge.identity.domain.User;
import com.tradebridge.identity.domain.UserRole;
import com.tradebridge.identity.repository.RoleRepository;
import com.tradebridge.identity.repository.UserRepository;
import com.tradebridge.identity.repository.UserRoleRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Loads user details for authentication. Kept in the service for the case
 * where internal calls need to look up by email — the primary auth path
 * is via {@link com.tradebridge.identity.service.AuthenticationService}.
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;

    public UserDetailsServiceImpl(UserRepository userRepository,
                                  UserRoleRepository userRoleRepository,
                                  RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
        this.roleRepository = roleRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = (username != null && username.startsWith("+"))
                ? userRepository.findByPhoneNumber(username).orElse(null)
                : userRepository.findByEmail(username == null ? "" : username.toLowerCase()).orElse(null);
        if (user == null) {
            throw new UsernameNotFoundException("User not found");
        }
        Set<SimpleGrantedAuthority> authorities = new HashSet<>();
        for (UserRole ur : userRoleRepository.findActiveByUserId(user.getId(), Instant.now())) {
            Role role = roleRepository.findById(ur.getRoleId()).orElse(null);
            if (role != null) {
                authorities.add(new SimpleGrantedAuthority(role.getCode()));
            }
        }
        return org.springframework.security.core.userdetails.User
                .withUsername(user.getId().toString())
                .password("")
                .authorities(authorities)
                .accountLocked(!user.getStatus().canAuthenticate())
                .disabled(user.getStatus() == com.tradebridge.identity.domain.enums.UserStatus.CLOSED)
                .build();
    }

    public User requireById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + userId));
    }
}
