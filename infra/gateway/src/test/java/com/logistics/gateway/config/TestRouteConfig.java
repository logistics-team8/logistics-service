package com.logistics.gateway.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class TestRouteConfig {

    @Bean
    RouteLocator testRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("test", r -> r.path("/**").uri("http://localhost:8080"))
                .build();
    }
}
