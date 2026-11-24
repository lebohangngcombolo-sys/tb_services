package com.tradebridge.identity.config;

import dev.samstevens.totp.code.CodeGenerator;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * TOTP primitives (spec §3.1D). The MfaService constructs its own
 * primitives inline, so this bean is optional — declared here so the
 * configuration is inspectable and swappable in tests.
 */
@Configuration
public class TotpConfig {

    @Bean
    public TimeProvider totpTimeProvider() {
        return new SystemTimeProvider();
    }

    @Bean
    public CodeGenerator totpCodeGenerator() {
        return new DefaultCodeGenerator(HashingAlgorithm.SHA1);
    }

    @Bean
    public DefaultCodeVerifier totpCodeVerifier(CodeGenerator generator, TimeProvider timeProvider,
                                                IdentityProperties properties) {
        DefaultCodeVerifier verifier = new DefaultCodeVerifier(generator, timeProvider);
        verifier.setTimePeriod((int) properties.getMfa().getTotpPeriod().toSeconds());
        verifier.setAllowedTimePeriodDiscrepancy(1);
        return verifier;
    }
}
