package com.logistics.userservice.infrastructure.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
        String accessSecret,
        String refreshSecret,
        boolean cookieSecure,
        Duration accessTokenExpiration,
        Duration refreshTokenExpiration) {}
