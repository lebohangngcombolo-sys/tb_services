package com.tradebridge.validation.annotation;

import com.tradebridge.validation.currency.CurrencyCodeValidator;
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
 * Validates an ISO 4217 currency code (case-insensitive).
 */
@Documented
@Constraint(validatedBy = ValidCurrencyCode.Validator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidCurrencyCode {
    String message() default "Invalid ISO 4217 currency code";
    Class<?>[] groups() default { };
    Class<? extends Payload>[] payload() default { };

    class Validator implements ConstraintValidator<ValidCurrencyCode, String> {
        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            if (value == null) {
                return false;
            }
            return CurrencyCodeValidator.isValid(value);
        }
    }
}
