package com.logistics.userservice.presentation;

import com.logistics.common.exception.BusinessException;
import com.logistics.common.response.ApiResponse;
import com.logistics.userservice.application.AuthService;
import com.logistics.userservice.application.token.TokenResult;
import com.logistics.userservice.infrastructure.security.JwtProperties;
import com.logistics.userservice.presentation.dto.auth.LoginRequest;
import com.logistics.userservice.presentation.dto.auth.TokenResponse;
import com.logistics.userservice.presentation.exception.AuthErrorCode;
import com.logistics.userservice.presentation.swagger.AuthApi;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

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
        TokenResult tokenResult = authService.login(request.toCommand());

        var cookie =
                createRefreshToken(
                        tokenResult.refreshToken(),
                        jwtProperties.refreshTokenExpirationInSeconds());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(ApiResponse.success(TokenResponse.from(tokenResult.accessToken())));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest request) {
        String accessToken = resolveAccessToken(request);

        if (!StringUtils.hasText(accessToken)) {
            throw new BusinessException(AuthErrorCode.TOKEN_INVALID);
        }

        authService.logout(accessToken);

        var cookie = createRefreshToken("", 0);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(ApiResponse.success(null));
    }

    @PostMapping("/reissue")
    public ResponseEntity<ApiResponse<TokenResponse>> reissue(
            @CookieValue(value = "refreshToken", required = false) String refreshToken) {
        if (!StringUtils.hasText(refreshToken)) {
            throw new BusinessException(AuthErrorCode.TOKEN_INVALID);
        }

        TokenResult tokenResult = authService.reissue(refreshToken);

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

    /**
     * Access Token 접두사 제거
     *
     * @param request
     * @return String Access Token
     */
    private String resolveAccessToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }

        return null;
    }
}
