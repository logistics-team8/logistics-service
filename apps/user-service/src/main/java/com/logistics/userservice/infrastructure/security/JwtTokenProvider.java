package com.logistics.userservice.infrastructure.security;

import com.logistics.userservice.application.dto.TokenClaims;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import javax.crypto.SecretKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {
    private final JwtProperties jwtProperties;

    /**
     * JWT 토큰 생성
     *
     * @param tokenClaims 클레임에 담을 사용자 정보
     * @param sessionId Random UUID
     * @param signingKey
     * @param validity
     * @return Access Token || Refresh Token
     */
    private String generateToken(
            TokenClaims tokenClaims, UUID sessionId, Key signingKey, long validity) {
        Map<String, Object> claims = new HashMap<>();
        Date date = new Date();

        claims.put("sessionId", sessionId.toString());
        claims.put(
                "hubId",
                tokenClaims.hubId() != null ? tokenClaims.hubId().toString() : null); // null 주의
        claims.put(
                "companyId",
                tokenClaims.companyId() != null
                        ? tokenClaims.companyId().toString()
                        : null); // null 주의

        return Jwts.builder()
                .subject(tokenClaims.userId().toString())
                .claims(claims)
                .issuedAt(date)
                .expiration(new Date(date.getTime() + validity))
                .signWith(signingKey)
                .compact();
    }

    public String generateAccessToken(TokenClaims tokenClaims, UUID sessionId, long validity) {
        SecretKey signingKey = createSigningKey(jwtProperties.accessSecret());
        return generateToken(tokenClaims, sessionId, signingKey, validity);
    }

    public String generateRefreshToken(TokenClaims tokenClaims, UUID sessionId, long validity) {
        SecretKey signingKey = createSigningKey(jwtProperties.refreshSecret());
        return generateToken(tokenClaims, sessionId, signingKey, validity);
    }

    /**
     * JWT 토큰에서 UserId 파싱
     *
     * @param token JWT 토큰
     * @return sub 값 반환 - User PK
     */
    public UUID getUserIdFromToken(String token) {
        return UUID.fromString(getClaimFromToken(token, Claims::getSubject));
    }

    // Session ID 파싱 - Access Token
    public UUID getSessionIdFromAccessToken(String accessToken) {
        Claims claims = getAllClaimsFromToken(accessToken);
        return UUID.fromString(claims.get("sessionId", String.class));
    }

    // Session ID 파싱 - Refresh Token
    public UUID getSessionIdFromRefreshToken(String refreshToken) {
        Claims claims = getAllClaimsFromRefreshToken(refreshToken);
        return UUID.fromString(claims.get("sessionId", String.class));
    }

    // Access Token 유효기간 검증
    public Date getExpirationDateFromToken(String token) {
        return getClaimFromToken(token, Claims::getExpiration);
    }

    /**
     * JWT TOKEN에서 특정 Claims 추출
     *
     * @param token JWT 토큰
     * @param claimsResolver 람다식
     * @return 추출 값
     * @param <T>
     */
    public <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
        Claims claims = getAllClaimsFromToken(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Access Token 모든 Claims 파싱 후 반환
     *
     * @param token
     * @return Claims 값
     */
    public Claims getAllClaimsFromToken(String token) {
        SecretKey signingKey = createSigningKey(jwtProperties.accessSecret());
        return getAllClaimsFromToken(token, signingKey);
    }

    /**
     * Refresh Token 모든 Claims 파싱 후 반환
     *
     * @param token
     * @return Claims 값
     */
    public Claims getAllClaimsFromRefreshToken(String token) {
        SecretKey signingKey = createSigningKey(jwtProperties.refreshSecret());
        return getAllClaimsFromToken(token, signingKey);
    }

    /**
     * 토큰 검증 후 Claims 추출
     *
     * @param token JWT 토큰
     * @param signingKey SecretKey
     * @return Claims 값
     */
    public Claims getAllClaimsFromToken(String token, SecretKey signingKey) {
        return Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token).getPayload();
    }

    // Access Token 접두사 제거 - Service
    public String resolveAccessToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(HttpHeaders.AUTHORIZATION);
        return resolveBearerToken(bearerToken);
    }

    private String resolveBearerToken(String bearerToken) {
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    private SecretKey createSigningKey(String secret) {

        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }
}
