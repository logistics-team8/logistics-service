package com.logistics.userservice.infrastructure.security;

import com.logistics.userservice.application.token.TokenClaims;
import com.logistics.userservice.application.token.TokenPayload;
import com.logistics.userservice.application.token.TokenProvider;
import com.logistics.userservice.infrastructure.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.crypto.SecretKey;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class JwtTokenProvider implements TokenProvider {
    private final JwtProperties jwtProperties;

    private static final String SESSION_ID = "sessionId";
    private static final String HUB_ID = "hubId";
    private static final String COMPANY_ID = "companyId";

    /**
     * Access Token 생성
     *
     * @param tokenClaims
     * @param sessionId
     * @return String Access Token
     */
    @Override
    public String generateAccessToken(TokenClaims tokenClaims, UUID sessionId) {
        SecretKey signingKey = createSigningKey(jwtProperties.accessSecret());
        long validity = jwtProperties.accessTokenExpiration().toMillis();

        return generateToken(tokenClaims, sessionId, signingKey, validity);
    }

    /**
     * Refresh Token 생성
     *
     * @param tokenClaims
     * @param sessionId
     * @return String Refresh Token
     */
    @Override
    public String generateRefreshToken(TokenClaims tokenClaims, UUID sessionId) {
        SecretKey signingKey = createSigningKey(jwtProperties.refreshSecret());
        long validity = jwtProperties.refreshTokenExpiration().toMillis();

        return generateToken(tokenClaims, sessionId, signingKey, validity);
    }

    /**
     * Access Token Claims 파싱
     *
     * @param accessToken
     * @return TokenPayload DTO
     */
    @Override
    public TokenPayload getAllClaimsFromAccessToken(String accessToken) {
        Claims claims = parseToken(accessToken, createSigningKey(jwtProperties.accessSecret()));

        return toTokenPayload(claims);
    }

    /**
     * Refresh Token Claims 파싱
     *
     * @param refreshToken
     * @return TokenPayload DTO
     */
    @Override
    public TokenPayload getAllClaimsFromRefreshToken(String refreshToken) {
        Claims claims = parseToken(refreshToken, createSigningKey(jwtProperties.refreshSecret()));

        return toTokenPayload(claims);
    }

    /**
     * Access Token 만료기간 확인
     *
     * @param accessToken
     * @return 만료기간
     */
    @Override
    public Date getExpirationFromAccessToken(String accessToken) {
        Claims claims = parseToken(accessToken, createSigningKey(jwtProperties.accessSecret()));

        return claims.getExpiration();
    }

    /**
     * Token 생성
     *
     * @param tokenClaims
     * @param sessionId
     * @param signingKey
     * @param validity
     * @return Jwt Token
     */
    private String generateToken(
            TokenClaims tokenClaims, UUID sessionId, SecretKey signingKey, long validity) {
        UUID userId = tokenClaims.userId();
        UUID hubId = tokenClaims.hubId();
        UUID companyId = tokenClaims.companyId();

        Map<String, Object> claims = new HashMap<>();
        Date date = new Date();

        claims.put(SESSION_ID, sessionId.toString());
        claims.put(HUB_ID, hubId != null ? hubId.toString() : null); // null 주의
        claims.put(COMPANY_ID, companyId != null ? companyId.toString() : null); // null 주의

        return Jwts.builder()
                .subject(userId.toString())
                .claims(claims)
                .issuedAt(date)
                .expiration(new Date(date.getTime() + validity))
                .signWith(signingKey)
                .compact();
    }

    /**
     * Token 파싱
     *
     * @param token
     * @param signingKey
     * @return Claims
     */
    private Claims parseToken(String token, SecretKey signingKey) {
        return Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token).getPayload();
    }

    /**
     * DTO 객체 변환
     *
     * @param claims
     * @return TokenPayload
     */
    private TokenPayload toTokenPayload(Claims claims) {
        String hubId = claims.get(HUB_ID, String.class);
        String companyId = claims.get(COMPANY_ID, String.class);

        return new TokenPayload(
                UUID.fromString(claims.getSubject()),
                UUID.fromString(claims.get(SESSION_ID, String.class)),
                StringUtils.hasText(hubId) ? UUID.fromString(hubId) : null,
                StringUtils.hasText(companyId) ? UUID.fromString(companyId) : null);
    }

    /**
     * JWT 시크릿 키 생성
     *
     * @param secret
     * @return SecretKey
     */
    private SecretKey createSigningKey(String secret) {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }
}
