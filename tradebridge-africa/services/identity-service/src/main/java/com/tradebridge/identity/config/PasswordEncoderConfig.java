package com.tradebridge.identity.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Password encoder (spec §3.4).
 *
 * <p>BCrypt with strength 12 is used for portability. If Argon2id is
 * required (recommended for new systems), swap the bean to
 * {@code Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8()}. The
 * choice is deliberately centralized here so the strategy can be
 * upgraded without touching service code.
 */
@Configuration
public class PasswordEncoderConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
