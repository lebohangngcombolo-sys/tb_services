package com.tradebridge.gateway.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PublicRouteRegistryTest {

    private final PublicRouteRegistry registry = new PublicRouteRegistry();

    @Test
    void mfaLoginVerificationIsPublicOnlyForItsPostRoute() {
        assertThat(registry.isPublic("POST", "/api/v1/identity/mfa/verify")).isTrue();
        assertThat(registry.isPublic("GET", "/api/v1/identity/mfa/verify")).isFalse();
        assertThat(registry.isPublic("POST", "/api/v1/identity/mfa/verify/other")).isFalse();
    }
}