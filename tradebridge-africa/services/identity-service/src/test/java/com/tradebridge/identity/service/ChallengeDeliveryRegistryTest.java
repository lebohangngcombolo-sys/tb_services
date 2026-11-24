package com.tradebridge.identity.service;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class ChallengeDeliveryRegistryTest {

    @Test
    void consumesCodeAndDestinationExactlyOnce() {
        UUID challengeId = UUID.randomUUID();
        RegistrationService.ChallengeDeliveryRegistry.register(
                challengeId, "123456", "user@example.com", Duration.ofMinutes(1));

        RegistrationService.ChallengeDeliveryRegistry.Entry delivery =
                RegistrationService.ChallengeDeliveryRegistry.consume(challengeId);

        assertNotNull(delivery);
        assertEquals("123456", delivery.otp());
        assertEquals("user@example.com", delivery.destination());
        assertNull(RegistrationService.ChallengeDeliveryRegistry.consume(challengeId));
    }
}
