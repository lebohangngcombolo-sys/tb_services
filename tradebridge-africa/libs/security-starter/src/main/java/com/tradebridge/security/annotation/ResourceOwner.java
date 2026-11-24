package com.tradebridge.security.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marker indicating that the annotated method must run an ownership check
 * <i>in the owning service</i> against the resource identified by the
 * parameter at {@link #paramIndex()} (a UUID).
 *
 * <p>The spec is explicit (§2.2): "Never rely on the Gateway alone for
 * resource ownership or business-level permissions." The actual check
 * lives in the owning service; this annotation only declares the
 * intent so that {@code @ResourceOwner} can be intercepted to raise
 * a generic ownership exception when a custom {@code ResourceOwnershipChecker}
 * is registered.
 *
 * <p>If no {@code ResourceOwnershipChecker} bean is present, the annotation
 * is a no-op — services with richer authorization (e.g. Customer Service
 * {@code OrganizationAccessGuard}) should perform the check explicitly.
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ResourceOwner {
    /** 0-based index of the parameter that holds the resource UUID. */
    int paramIndex() default 0;
    /** Human-readable resource name, used in exception messages. */
    String resource() default "resource";
}
