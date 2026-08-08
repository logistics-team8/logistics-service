package com.logistics.userservice.presentation;

import com.logistics.common.response.ApiResponse;
import com.logistics.userservice.application.AuthService;
import com.logistics.userservice.application.dto.TokenResult;
import com.logistics.userservice.infrastructure.security.JwtProperties;
import com.logistics.userservice.presentation.dto.request.LoginRequest;
import com.logistics.userservice.presentation.dto.response.TokenResponse;
import com.logistics.userservice.presentation.swagger.AuthApi;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController implements AuthApi {
    private final AuthService authService;
    private final JwtProperties jwtProperties;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenResponse>> login(
            @Valid @RequestBody LoginRequest request) {
        TokenResult tokenResult = authService.login(request);

        var cookie =
                createRefreshToken(
                        tokenResult.refreshToken(),
                        jwtProperties.refreshTokenExpirationInSeconds());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(ApiResponse.success(TokenResponse.from(tokenResult.accessToken())));
    }

    /**
     * Refresh Token 쿠키 생성
     *
     * @param refreshToken Service로부터 반환받은 토큰
     * @param maxAge 유효시간 Seconds
     * @return Refresh Token이 담긴 Cookie 반환
     */
    private ResponseCookie createRefreshToken(String refreshToken, long maxAge) {
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
