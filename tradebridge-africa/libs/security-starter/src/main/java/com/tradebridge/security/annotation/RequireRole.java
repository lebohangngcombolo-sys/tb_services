package com.tradebridge.security.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Requires the authenticated principal to possess at least one of the given
 * role codes (e.g. {@code KYC_ANALYST}, {@code ADMIN}).
 *
 * <p>Roles are considered through the configured hierarchy — a SUPER_ADMIN
 * passes a {@code @RequireRole("ADMIN")} check if the hierarchy implies it.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireRole {
    String[] value();
    boolean all() default false;
}
