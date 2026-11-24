package com.tradebridge.identity.config;

import com.tradebridge.common.response.ApiResponse;
import com.tradebridge.common.response.ResponseFactory;
import com.tradebridge.identity.service.RegistrationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

/**
 * Internal, single-use challenge delivery.
 *
 * <p>The notification service calls this endpoint when it needs the raw
 * OTP and raw destination after receiving a {@code *.requested.v1} event.
 * Both values are returned exactly once; the endpoint consumes the challenge
 * on read.
 *
 * <p>Not exposed publicly — the Gateway allowlist does not include
 * {@code /api/v1/identity/internal/**}.
 */
@RestController
@RequestMapping("/api/v1/identity/internal/challenge-delivery")
public class ChallengeDeliveryController {

    @GetMapping("/{challengeId}")
    public ResponseEntity<ApiResponse<Map<String, String>>> deliver(@PathVariable UUID challengeId) {
        RegistrationService.ChallengeDeliveryRegistry.Entry challenge =
                RegistrationService.ChallengeDeliveryRegistry.consume(challengeId);
        if (challenge == null || challenge.destination() == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseFactory.ok(null, "Challenge not found or already consumed"));
        }
        return ResponseEntity.ok(ResponseFactory.ok(Map.of(
                "code", challenge.otp(),
                "destination", challenge.destination())));
    }
}
