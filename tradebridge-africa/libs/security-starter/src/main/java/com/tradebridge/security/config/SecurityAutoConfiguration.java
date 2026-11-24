package com.tradebridge.security.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradebridge.security.annotation.RequirePermission;
import com.tradebridge.security.annotation.RequireRole;
import com.tradebridge.security.audit.AuditEventPublisher;
import com.tradebridge.security.audit.SecurityAuditHook;
import com.tradebridge.security.authorization.PermissionEvaluator;
import com.tradebridge.security.authorization.RoleHierarchy;
import com.tradebridge.security.client.ServiceIdentityProperties;
import com.tradebridge.security.client.ServiceTokenProvider;
import com.tradebridge.security.config.MethodSecurityConfig;
import com.tradebridge.security.jwt.JwksProvider;
import com.tradebridge.security.jwt.JwtDecoderFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ResourceLoader;
import org.springframework.security.oauth2.jwt.JwtDecoder;

/**
 * Root auto-configuration for the security starter.
 *
 * <p>Wires together:
 * <ul>
 *   <li>{@link SecurityProperties} and {@link ServiceIdentityProperties}</li>
 *   <li>{@link RoleHierarchy} + {@link PermissionEvaluator}</li>
 *   <li>{@link MethodSecurityConfig} AOP advice</li>
 *   <li>{@link AuditEventPublisher} + {@link SecurityAuditHook}</li>
 *   <li>{@link ServiceTokenProvider} (only when a private key is configured)</li>
 * </ul>
 *
 * <p>{@link JwtAutoConfiguration} is imported separately because it depends on
 * the JWT infrastructure and should be skipped in the Gateway (which manages
 * its own reactive decoder).
 */
@AutoConfiguration
@ConditionalOnClass(name = "org.springframework.security.core.Authentication")
@EnableConfigurationProperties({SecurityProperties.class, ServiceIdentityProperties.class})
@Import({JwtAutoConfiguration.class})
public class SecurityAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public RoleHierarchy roleHierarchy(SecurityProperties properties) {
        return new RoleHierarchy(properties.getPermissions().getHierarchy());
    }

    @Bean
    @ConditionalOnMissingBean
    public PermissionEvaluator permissionEvaluator(RoleHierarchy roleHierarchy) {
        return new PermissionEvaluator(roleHierarchy);
    }

    @Bean
    @ConditionalOnMissingBean
    public MethodSecurityConfig methodSecurityConfig(PermissionEvaluator permissionEvaluator) {
        return new MethodSecurityConfig(permissionEvaluator);
    }

    @Bean
    @ConditionalOnMissingBean
    public AuditEventPublisher auditEventPublisher(SecurityProperties properties) {
        return new AuditEventPublisher();
    }

    @Bean
    @ConditionalOnMissingBean
    public SecurityAuditHook securityAuditHook(AuditEventPublisher publisher, SecurityProperties properties) {
        // The hook is a no-op when audit is disabled; simplest is to always create it
        // and let AuditEventPublisher decide (future extension).
        return new SecurityAuditHook(publisher);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "tradebridge.security.service-identity",
            name = "private-key-location")
    public ServiceTokenProvider serviceTokenProvider(ServiceIdentityProperties properties,
                                                     ResourceLoader resourceLoader) {
        return new ServiceTokenProvider(properties, resourceLoader);
    }
}
