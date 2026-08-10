package com.logistics.userservice.presentation;

import com.logistics.common.exception.BusinessException;
import com.logistics.common.response.ApiResponse;
import com.logistics.userservice.application.AuthService;
import com.logistics.userservice.application.token.TokenResult;
import com.logistics.userservice.error.AuthErrorCode;
import com.logistics.userservice.presentation.cookie.CookieProvider;
import com.logistics.userservice.presentation.dto.auth.LoginRequest;
import com.logistics.userservice.presentation.dto.auth.TokenResponse;
import com.logistics.userservice.presentation.swagger.AuthApi;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController implements AuthApi {
    private final AuthService authService;
    private final CookieProvider cookieProvider;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenResponse>> login(
            @Valid @RequestBody LoginRequest request) {
        TokenResult tokenResult = authService.login(request.toCommand());

        var cookie = cookieProvider.createRefreshToken(tokenResult.refreshToken());

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

        var cookie = cookieProvider.deleteRefreshToken();

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

        var cookie = cookieProvider.createRefreshToken(tokenResult.refreshToken());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(ApiResponse.success(TokenResponse.from(tokenResult.accessToken())));
    }

    // ============================== Helper Method ====================================
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
