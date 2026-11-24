package com.tradebridge.security.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Requires the authenticated principal to possess at least one of the given
 * permission codes (see {@link com.tradebridge.security.authorization.PermissionCode}).
 *
 * <p>Enforced by {@link com.tradebridge.security.config.MethodSecurityConfig}
 * via a Spring AOP advice. Failures throw
 * {@code org.springframework.security.access.AccessDeniedException}.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequirePermission {
    String[] value();
    /** If true, requires ALL listed permissions; otherwise ANY is enough. */
    boolean all() default false;
}
