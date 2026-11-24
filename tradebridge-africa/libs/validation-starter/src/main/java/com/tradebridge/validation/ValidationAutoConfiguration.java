package com.tradebridge.validation;

import org.springframework.boot.autoconfigure.AutoConfiguration;

/**
 * Auto-configuration entry point for the validation starter.
 *
 * <p>All validators in this module are static utility methods, so this
 * class is essentially a marker. It exists so that:
 * <ul>
 *   <li>The {@code AutoConfiguration.imports} file has a stable target.</li>
 *   <li>Spring Boot recognizes this module as providing configuration
 *       (helps with ordering when this library is combined with others).</li>
 * </ul>
 *
 * <p>If a service needs to expose a {@code Validator} bean with the
 * module's default configuration, it declares that bean itself — this
 * starter does not override Spring's `LocalValidatorFactoryBean`.
 */
@AutoConfiguration
public class ValidationAutoConfiguration {
}
