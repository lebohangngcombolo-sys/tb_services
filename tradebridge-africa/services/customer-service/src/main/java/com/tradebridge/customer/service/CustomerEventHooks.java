package com.tradebridge.customer.service;

import com.tradebridge.customer.domain.CustomerProfile;
import com.tradebridge.customer.repository.CustomerProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

/**
 * Business logic for each consumed event (spec §6.3 "Consumed events").
 *
 * <p>Design intent:
 * <ul>
 *   <li>User registered → establish pending profile association.</li>
 *   <li>User activated → activate any pending customer profile.</li>
 *   <li>User status changed → apply access restriction locally.</li>
 *   <li>Country config published → invalidate any cached requirements
 *       (currently a no-op — the service always fetches fresh from
 *       Country Service on case creation, but the hook exists to make
 *       future caching straightforward).</li>
 * </ul>
 */
@Service
public class CustomerEventHooks {

    private static final Logger log = LoggerFactory.getLogger(CustomerEventHooks.class);

    private final CustomerProfileRepository profileRepository;

    public CustomerEventHooks(CustomerProfileRepository profileRepository) {
        this.profileRepository = profileRepository;
    }

    // ============================================================ IDENTITY

    @Transactional
    public void onUserRegistered(Map<String, Object> payload, UUID correlationId) {
        String userId = str(payload, "userId");
        String countryCode = str(payload, "countryCode");
        if (userId == null || countryCode == null) {
            log.warn("identity.user.registered payload missing required fields");
            return;
        }
        UUID identityUserId = UUID.fromString(userId);

        if (profileRepository.existsByIdentityUserId(identityUserId)) {
            log.debug("Customer profile already exists for identity user {} — skipping", identityUserId);
            return;
        }

        // The customer profile starts in PENDING state. Full activation
        // happens after the user completes onboarding (identity activation
        // or explicit profile creation). We create a stub now so downstream
        // flows can reference the identity user without racing.
        CustomerProfile profile = new CustomerProfile(
                UUID.randomUUID(),
                identityUserId,
                "INDIVIDUAL", // default; user may switch to COMPANY_REPRESENTATIVE later
                countryCode,
                "en");
        profileRepository.save(profile);
        log.info("Created pending customer profile for identity user {}", identityUserId);
    }

    @Transactional
    public void onUserActivated(Map<String, Object> payload, UUID correlationId) {
        String userId = str(payload, "userId");
        if (userId == null) return;
        UUID identityUserId = UUID.fromString(userId);
        profileRepository.findByIdentityUserId(identityUserId).ifPresent(profile -> {
            if (profile.getStatus() != com.tradebridge.customer.domain.enums.CustomerStatus.ACTIVE) {
                profile.activate();
                profileRepository.save(profile);
                log.info("Activated customer profile {} for identity user {}",
                        profile.getId(), identityUserId);
            }
        });
    }

    @Transactional
    public void onUserStatusChanged(Map<String, Object> payload, UUID correlationId) {
        String userId = str(payload, "userId");
        String newStatus = str(payload, "newStatus");
        if (userId == null || newStatus == null) return;
        UUID identityUserId = UUID.fromString(userId);
        profileRepository.findByIdentityUserId(identityUserId).ifPresent(profile -> {
            switch (newStatus) {
                case "SUSPENDED" -> profile.suspend();
                case "CLOSED" -> profile.close();
                case "ACTIVE" -> profile.activate();
                default -> { return; }
            }
            profileRepository.save(profile);
            log.info("Customer profile {} status synced to {}", profile.getId(), newStatus);
        });
    }

    // ============================================================ COUNTRY

    @Transactional
    public void onCountryConfigurationPublished(Map<String, Object> payload, UUID correlationId) {
        String countryCode = str(payload, "countryCode");
        Object version = payload.get("version");
        // No cached state to invalidate today. Future enhancement: if
        // CustomerService caches country requirements, evict them here.
        log.info("Country configuration published: {} v{} — no cached state to invalidate",
                countryCode, version);
    }

    // ============================================================ helpers

    private static String str(Map<String, Object> payload, String key) {
        Object v = payload == null ? null : payload.get(key);
        return v == null ? null : v.toString();
    }
}