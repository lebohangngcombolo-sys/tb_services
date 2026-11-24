package com.tradebridge.identity.security;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Public JWKS endpoint (spec §2.2, §11 decision 1).
 *
 * <p>All downstream services and the API Gateway fetch their JWT
 * verification keys from here. This endpoint is on the Gateway's
 * public allowlist and must remain unauthenticated.
 */
@RestController
public class JwksController {

    private final JwtKeyProvider keyProvider;

    public JwksController(JwtKeyProvider keyProvider) {
        this.keyProvider = keyProvider;
    }

    @GetMapping(value = "/.well-known/jwks.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> jwks() {
        return ResponseEntity.ok()
                .header("Cache-Control", "public, max-age=300")
                .body(keyProvider.jwks());
    }
}
