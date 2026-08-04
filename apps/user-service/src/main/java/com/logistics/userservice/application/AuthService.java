package com.logistics.userservice.application;

import com.logistics.common.error.CommonErrorCode;
import com.logistics.common.exception.BusinessException;
import com.logistics.userservice.application.dto.TokenResult;
import com.logistics.userservice.domain.User;
import com.logistics.userservice.domain.UserRepository;
import com.logistics.userservice.infrastructure.security.JwtProperties;
import com.logistics.userservice.infrastructure.security.JwtTokenProvider;
import com.logistics.userservice.presentation.dto.request.LoginRequest;
import com.logistics.userservice.presentation.exception.AuthErrorCode;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class AuthService {
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;

    @Transactional(readOnly = true)
    public TokenResult login(LoginRequest request) {
        User user =
                userRepository
                        .findByUsernameAndDeletedAtIsNull(request.username())
                        .orElseThrow(() -> new BusinessException(AuthErrorCode.INVALID_LOGIN));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BusinessException(AuthErrorCode.INVALID_LOGIN);
        }

        // TODO : 사용자 상태 검증 로직 (PENDING)
        //        개발 어느정도 진행됐을 시 주석 해제
        // user.validateActive();

        log.info("[AuthService] 사용자 인증 성공: {}", user.getUsername());

        return createAuthResponse(user);
    }

    /**
     * 액세스 토큰, 리프래시 토큰 생성 후, Redis에 Refresh Token 저장한 뒤 TokenResult 반환
     *
     * @param user 사용자 정보
     * @return TokenResult (AccessToken, RefreshToken)
     */
    private TokenResult createAuthResponse(User user) {
        try {
            UUID sessionId = UUID.randomUUID();

            String accessToken =
                    jwtTokenProvider.generateAccessToken(
                            user, sessionId, jwtProperties.accessTokenExpirationInMillis());

            String refreshToken =
                    jwtTokenProvider.generateRefreshToken(
                            user, sessionId, jwtProperties.refreshTokenExpirationInMillis());

            // TODO : Redis 도입 시 Refresh Token 저장 로직 추가
            // List<String> key = Collections.singletonList("user:sessions:" + userid);
            log.info("[AuthService] 토큰 발급 완료: {}", user.getUsername());

            return new TokenResult(accessToken, refreshToken);
        } catch (Exception e) {
            log.error("[AuthService] 토큰 발급 중 에러 발생", e);
            throw new BusinessException(CommonErrorCode.INTERNAL_SERVER_ERROR);
        }
    }
}
