package com.logistics.gateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JwtTokenProvider {
    private final JwtProperties jwtProperties;

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
     * 토큰 검증 후 Claims 추출
     *
     * @param token JWT 토큰
     * @param signingKey SecretKey
     * @return Claims 값
     */
    public Claims getAllClaimsFromToken(String token, SecretKey signingKey) {
        return Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token).getPayload();
    }

    private SecretKey createSigningKey(String secret) {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }
}
