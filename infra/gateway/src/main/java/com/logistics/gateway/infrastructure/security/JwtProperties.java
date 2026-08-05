package com.logistics.gateway.infrastructure.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
        String accessSecret,
        String refreshSecret,
        boolean cookieSecure,
        Duration accessTokenExpiration,
        Duration refreshTokenExpiration) {
    public long accessTokenExpirationInMillis() {
        return accessTokenExpiration.toMillis();
    }

    public long refreshTokenExpirationInMillis() {
        return refreshTokenExpiration.toMillis();
    }

    public long refreshTokenExpirationInSeconds() {
        return refreshTokenExpiration.toSeconds();
    }
}
