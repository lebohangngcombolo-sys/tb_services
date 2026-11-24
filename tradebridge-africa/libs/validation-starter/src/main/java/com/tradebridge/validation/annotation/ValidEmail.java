package com.tradebridge.validation.annotation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validates an email address and reports a normalized value when the
 * annotated field is an {@code AtomicReference<String>}? No — normalization
 * is done in the service layer via {@link com.tradebridge.validation.email.EmailValidator#normalize(String)}.
 *
 * <p>This constraint only checks <b>format</b>. Uniqueness and account-existence
 * checks happen in the service layer.
 */
@Documented
@Constraint(validatedBy = { })
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@NotNull(message = "Email is required")
@Size(max = 254, message = "Email must not exceed 254 characters")
@jakarta.validation.constraints.Pattern(
        regexp = "^(?!\\.)(?!.*\\.\\.)[A-Za-z0-9!#$%&'*+/=?^_`{|}~.-]{1,64}(?<!\\.)@"
                + "(?=.{1,255}$)(?:[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?\\.)+[A-Za-z]{2,63}$",
        message = "Invalid email address"
)
public @interface ValidEmail {
    String message() default "Invalid email address";
    Class<?>[] groups() default { };
    Class<? extends Payload>[] payload() default { };
}
