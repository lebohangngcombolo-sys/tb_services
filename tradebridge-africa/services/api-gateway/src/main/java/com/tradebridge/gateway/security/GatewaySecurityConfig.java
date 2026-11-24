
package com.tradebridge.gateway.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradebridge.common.exception.ErrorCode;
import com.tradebridge.common.response.ResponseFactory;
import com.tradebridge.gateway.telemetry.RejectionLogger;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;

import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;

import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.security.web.server.authorization.ServerAccessDeniedHandler;

import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.web.server.authorization.AuthorizationContext;

import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

@Configuration
@EnableWebFluxSecurity
public class GatewaySecurityConfig {

    private final PublicRouteRegistry publicRoutes;
    private final AuthorizationRuleEvaluator authorizationRules;
    private final RejectionLogger rejectionLogger;
    private final ObjectMapper objectMapper;

    public GatewaySecurityConfig(
        PublicRouteRegistry publicRoutes,
        AuthorizationRuleEvaluator authorizationRules,
        RejectionLogger rejectionLogger,
        ObjectMapper objectMapper) {

        this.publicRoutes = publicRoutes;
        this.authorizationRules = authorizationRules;
        this.rejectionLogger = rejectionLogger;
        this.objectMapper = objectMapper;
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(
        ServerHttpSecurity http) {

        return http
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
            .formLogin(ServerHttpSecurity.FormLoginSpec::disable)

            .authorizeExchange(auth -> auth
                .pathMatchers(
                    "/actuator/health",
                    "/actuator/health/**",
                    "/actuator/info"
                ).permitAll()

                .pathMatchers("/.well-known/jwks.json").permitAll()

                .pathMatchers(
                    "/v3/api-docs/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html"
                ).permitAll()

                .anyExchange().access(this::authorize)
            )

            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .jwtAuthenticationConverter(
                        reactiveJwtAuthConverter()
                    )
                )
                .authenticationEntryPoint(unauthorizedEntryPoint())
                .accessDeniedHandler(accessDeniedHandler())
            )

            .build();
    }

    /**
     * Public routes bypass authentication.
     * All other routes require an authenticated JWT.
     */
    private Mono<AuthorizationDecision> authorize(
        Mono<Authentication> authentication,
        AuthorizationContext context) {

        ServerWebExchange exchange = context.getExchange();

        String path = exchange.getRequest()
            .getURI()
            .getPath();

        String method = exchange.getRequest()
            .getMethod()
            .name();

        if (publicRoutes.isPublic(method, path)) {
            return Mono.just(new AuthorizationDecision(true));
        }

        return authentication
            .filter(Authentication::isAuthenticated)
            .map(auth -> {

                if (auth.getPrincipal() instanceof Jwt jwt
                    && !authorizationRules.isAllowed(jwt, path)) {

                    rejectionLogger.logRejection(
                        exchange,
                        "AUTHORIZATION_DENIED",
                        path
                    );

                    return new AuthorizationDecision(false);
                }

                return new AuthorizationDecision(true);
            })
            .defaultIfEmpty(new AuthorizationDecision(false));
    }

    /**
     * Converts JWT roles into Spring Security authorities.
     *
     * The adapter converts the servlet-style converter into
     * the reactive converter required by Spring WebFlux.
     */
    private Converter<Jwt, Mono<AbstractAuthenticationToken>>
    reactiveJwtAuthConverter() {

        JwtGrantedAuthoritiesConverter authoritiesConverter =
            new JwtGrantedAuthoritiesConverter();

        authoritiesConverter.setAuthoritiesClaimName("roles");
        authoritiesConverter.setAuthorityPrefix("");

        JwtAuthenticationConverter authenticationConverter =
            new JwtAuthenticationConverter();

        authenticationConverter.setJwtGrantedAuthoritiesConverter(
            authoritiesConverter
        );

        return new ReactiveJwtAuthenticationConverterAdapter(
            authenticationConverter
        );
    }

    /**
     * Handles unauthenticated requests with the standard error envelope.
     */
    private ServerAuthenticationEntryPoint unauthorizedEntryPoint() {

        return (exchange, ex) -> {

            rejectionLogger.logRejection(
                exchange,
                "UNAUTHORIZED",
                exchange.getRequest().getURI().getPath()
            );

            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);

            exchange.getResponse()
                .getHeaders()
                .setContentType(MediaType.APPLICATION_JSON);

            try {

                byte[] bytes = objectMapper.writeValueAsBytes(
                    ResponseFactory.error(
                        ErrorCode.UNAUTHORIZED,
                        "Authentication required"
                    )
                );

                DataBuffer buffer = exchange.getResponse()
                    .bufferFactory()
                    .wrap(bytes);

                return exchange.getResponse()
                    .writeWith(Mono.just(buffer));

            } catch (Exception e) {
                return exchange.getResponse().setComplete();
            }
        };
    }

    /**
     * Handles authenticated users who lack authorization.
     */
    private ServerAccessDeniedHandler accessDeniedHandler() {

        return (exchange, ex) -> {

            rejectionLogger.logRejection(
                exchange,
                "FORBIDDEN",
                exchange.getRequest().getURI().getPath()
            );

            exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);

            exchange.getResponse()
                .getHeaders()
                .setContentType(MediaType.APPLICATION_JSON);

            try {

                byte[] bytes = objectMapper.writeValueAsBytes(
                    ResponseFactory.error(
                        ErrorCode.FORBIDDEN,
                        "Access denied"
                    )
                );

                DataBuffer buffer = exchange.getResponse()
                    .bufferFactory()
                    .wrap(bytes);

                return exchange.getResponse()
                    .writeWith(Mono.just(buffer));

            } catch (Exception e) {
                return exchange.getResponse().setComplete();
            }
        };
    }
}
