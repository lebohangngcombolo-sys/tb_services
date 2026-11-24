package com.tradebridge.validation.annotation;

import com.tradebridge.validation.country.CountryCodeValidator;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validates an ISO 3166-1 alpha-2 or alpha-3 country code (case-insensitive).
 */
@Documented
@Constraint(validatedBy = ValidCountryCode.Validator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidCountryCode {
    /** Whether to allow 3-letter codes. Default true (alpha-2 or alpha-3). */
    boolean allowAlpha3() default true;

    String message() default "Invalid ISO 3166-1 country code";
    Class<?>[] groups() default { };
    Class<? extends Payload>[] payload() default { };

    class Validator implements ConstraintValidator<ValidCountryCode, String> {
        private boolean allowAlpha3;

        @Override
        public void initialize(ValidCountryCode annotation) {
            this.allowAlpha3 = annotation.allowAlpha3();
        }

        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            if (value == null) {
                return false;
            }
            return allowAlpha3
                    ? CountryCodeValidator.isValid(value)
                    : CountryCodeValidator.isValidAlpha2(value);
        }
    }
}
