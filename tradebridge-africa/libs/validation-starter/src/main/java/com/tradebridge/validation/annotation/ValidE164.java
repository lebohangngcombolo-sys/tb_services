package com.tradebridge.validation.annotation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validates an E.164 phone number (+country_code followed by 6-14 digits).
 */
@Documented
@Constraint(validatedBy = { })
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@NotNull(message = "Phone number is required")
@Size(min = 8, max = 16, message = "Phone number must be 8-16 characters")
@Pattern(regexp = "^\\+[1-9]\\d{6,14}$", message = "Invalid E.164 phone number")
public @interface ValidE164 {
    String message() default "Invalid E.164 phone number";
    Class<?>[] groups() default { };
    Class<? extends Payload>[] payload() default { };
}
