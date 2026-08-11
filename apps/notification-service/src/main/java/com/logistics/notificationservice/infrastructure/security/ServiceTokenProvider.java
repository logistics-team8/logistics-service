package com.logistics.notificationservice.infrastructure.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Component
public class ServiceTokenProvider {

    private final SecretKey secretKey;

    public ServiceTokenProvider(@Value("${service-token.secret}") String secret) {
        this.secretKey = Keys.hmacShaKeyFor(
                secret.getBytes(StandardCharsets.UTF_8)
        );
    }

    public String createToken(String serviceName) {

        return Jwts.builder()
                .subject(serviceName)
                .claim("type", "SERVICE")
                .issuedAt(new Date())
                .expiration(Date.from(Instant.now().plusSeconds(300)))
                .signWith(secretKey)
                .compact();
    }

    public boolean validate(String token) {

        try {
            Claims claims =
                    Jwts.parser()
                            .verifyWith(secretKey)
                            .build()
                            .parseSignedClaims(token)
                            .getPayload();

            return "SERVICE".equals(
                    claims.get("type", String.class)
            );

        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public String getServiceName(String token) {

        Claims claims =
                Jwts.parser()
                        .verifyWith(secretKey)
                        .build()
                        .parseSignedClaims(token)
                        .getPayload();

        return claims.getSubject();
    }

    public Claims parse(String token) {

        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}