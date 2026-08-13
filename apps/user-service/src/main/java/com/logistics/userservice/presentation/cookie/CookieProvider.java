package com.logistics.userservice.presentation.cookie;

import com.logistics.userservice.infrastructure.config.JwtProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CookieProvider {
    private final JwtProperties jwtProperties;

    /**
     * Refresh Token 쿠키 생성
     *
     * @param refreshToken
     * @return
     */
    public ResponseCookie createRefreshToken(String refreshToken) {
        return createRefreshToken(refreshToken, jwtProperties.refreshTokenExpiration().toSeconds());
    }

    /**
     * Refresh Token 쿠키 삭제
     *
     * @return Refresh Token이 담긴 Cookie 반환
     */
    public ResponseCookie deleteRefreshToken() {
        return createRefreshToken("", 0);
    }

    public ResponseCookie createRefreshToken(String refreshToken, long maxAge) {
        return ResponseCookie.from("refreshToken", refreshToken)
                .maxAge(maxAge)
                .path("/api/v1/auth")
                // TODO : https 적용 시 secure true로 수정해야함
                .secure(jwtProperties.cookieSecure())
                .sameSite("Strict")
                .httpOnly(true)
                .build();
    }
}
