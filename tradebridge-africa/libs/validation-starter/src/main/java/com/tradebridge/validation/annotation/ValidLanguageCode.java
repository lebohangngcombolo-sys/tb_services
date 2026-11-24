package com.tradebridge.validation.annotation;

import com.tradebridge.validation.language.LanguageCodeValidator;
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
 * Validates ISO 639-1 language codes and BCP 47 tags whose primary
 * subtag is ISO 639-1.
 */
@Documented
@Constraint(validatedBy = ValidLanguageCode.Validator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidLanguageCode {
    /** If false, only allow bare ISO 639-1 (no region subtag). Default true. */
    boolean allowBcp47() default true;

    String message() default "Invalid language code";
    Class<?>[] groups() default { };
    Class<? extends Payload>[] payload() default { };

    class Validator implements ConstraintValidator<ValidLanguageCode, String> {
        private boolean allowBcp47;

        @Override
        public void initialize(ValidLanguageCode annotation) {
            this.allowBcp47 = annotation.allowBcp47();
        }

        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            if (value == null) {
                return false;
            }
            return allowBcp47
                    ? LanguageCodeValidator.isValid(value)
                    : LanguageCodeValidator.isValidIso639_1(value);
        }
    }
}
