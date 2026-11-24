
package com.tradebridge.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RouteConfig {

    @Bean
    public RouteLocator gatewayRoutes(
        RouteLocatorBuilder builder,
        GatewayProperties properties) {

        RouteLocatorBuilder.Builder routes = builder.routes();

        if (properties.getRoutes().isEmpty()) {

            // Defensive default routes
            routes = routes
                .route("identity", r -> r
                    .path("/api/v1/identity/**")
                    .uri("http://localhost:8081"))

                .route("countries", r -> r
                    .path(
                        "/api/v1/countries/**",
                        "/api/v1/currencies",
                        "/api/v1/languages"
                    )
                    .uri("http://localhost:8082"))

                .route("notifications", r -> r
                    .path("/api/v1/notifications/**")
                    .uri("http://localhost:8083"))

                .route("customers", r -> r
                    .path(
                        "/api/v1/customers/**",
                        "/api/v1/organizations/**",
                        "/api/v1/kyc/**"
                    )
                    .uri("http://localhost:8084"));

        } else {

            for (GatewayProperties.RouteDefinition def : properties.getRoutes()) {

                routes = routes.route(def.getId(), r -> {

                    String[] paths = def.getPath() == null
                        ? new String[0]
                        : def.getPath().split(",");

                    var route = r.path(paths);

                    if (def.getStripPrefix() != null
                        && !def.getStripPrefix().isBlank()) {

                        int stripPrefix = Integer.parseInt(
                            def.getStripPrefix().trim()
                        );

                        return route
                            .filters(f -> f.stripPrefix(stripPrefix))
                            .uri(def.getUri());
                    }

                    return route.uri(def.getUri());
                });
            }
        }

        return routes.build();
    }
}
