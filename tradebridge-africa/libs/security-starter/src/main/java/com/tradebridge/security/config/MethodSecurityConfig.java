package com.tradebridge.security.config;

import com.tradebridge.security.annotation.RequirePermission;
import com.tradebridge.security.annotation.RequireRole;
import com.tradebridge.security.authorization.PermissionEvaluator;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * AOP advice for {@link RequireRole} and {@link RequirePermission}.
 *
 * <p>Kept as a standalone {@code @Aspect} (rather than Spring Security
 * {@code @PreAuthorize} SpEL) so the rule set is easy to audit and unit-test.
 */
@Aspect
public class MethodSecurityConfig {

    private final PermissionEvaluator permissionEvaluator;

    public MethodSecurityConfig(PermissionEvaluator permissionEvaluator) {
        this.permissionEvaluator = permissionEvaluator;
    }

    @Around("@annotation(com.tradebridge.security.annotation.RequireRole) "
            + "|| @within(com.tradebridge.security.annotation.RequireRole)")
    public Object enforceRole(ProceedingJoinPoint pjp) throws Throwable {
        RequireRole rule = resolve(pjp, RequireRole.class);
        if (rule != null) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            List<String> required = Arrays.asList(rule.value());
            boolean allowed = rule.all()
                    ? required.stream().allMatch(r -> permissionEvaluator.hasRole(auth, r))
                    : required.stream().anyMatch(r -> permissionEvaluator.hasRole(auth, r));
            if (!allowed) {
                throw new AccessDeniedException(
                        "Required role(s) not granted: " + required);
            }
        }
        return pjp.proceed();
    }

    @Around("@annotation(com.tradebridge.security.annotation.RequirePermission) "
            + "|| @within(com.tradebridge.security.annotation.RequirePermission)")
    public Object enforcePermission(ProceedingJoinPoint pjp) throws Throwable {
        RequirePermission rule = resolve(pjp, RequirePermission.class);
        if (rule != null) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            List<String> required = Arrays.asList(rule.value());
            boolean allowed = rule.all()
                    ? permissionEvaluator.hasAllPermissions(auth, required)
                    : permissionEvaluator.hasAnyPermission(auth, required);
            if (!allowed) {
                throw new AccessDeniedException(
                        "Required permission(s) not granted: " + required);
            }
        }
        return pjp.proceed();
    }

    private static <A extends java.lang.annotation.Annotation> A resolve(
            ProceedingJoinPoint pjp, Class<A> type) {
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        Method method = signature.getMethod();
        A annotation = method.getAnnotation(type);
        if (annotation == null) {
            annotation = pjp.getTarget().getClass().getAnnotation(type);
        }
        return annotation;
    }

    /** Convenience: aggregate validation helper used by both advices. */
    static boolean evaluate(Collection<String> required, java.util.function.Predicate<String> check, boolean all) {
        if (required == null || required.isEmpty()) {
            return true;
        }
        return all ? required.stream().allMatch(check) : required.stream().anyMatch(check);
    }
}
