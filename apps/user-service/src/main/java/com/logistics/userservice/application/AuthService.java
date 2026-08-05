package com.logistics.userservice.application;

import com.logistics.common.error.CommonErrorCode;
import com.logistics.common.exception.BusinessException;
import com.logistics.userservice.application.dto.TokenClaims;
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

        // TODO : 사용자 상태 검증 로직 (PENDING, REJECTED)
        //        개발 어느정도 진행됐을 시 주석 해제
        // user.validateActive();

        TokenClaims tokenClaims =
                new TokenClaims(user.getId(), user.getHubId(), user.getCompanyId());

        return createAuthResponse(tokenClaims);
    }

    /**
     * 액세스 토큰, 리프래시 토큰 생성 후, Redis에 Refresh Token 저장한 뒤 TokenResult 반환
     *
     * @param tokenClaims 사용자 정보
     * @return TokenResult (AccessToken, RefreshToken)
     */
    private TokenResult createAuthResponse(TokenClaims tokenClaims) {
        try {
            UUID sessionId = UUID.randomUUID();

            String accessToken =
                    jwtTokenProvider.generateAccessToken(
                            tokenClaims, sessionId, jwtProperties.accessTokenExpirationInMillis());

            String refreshToken =
                    jwtTokenProvider.generateRefreshToken(
                            tokenClaims, sessionId, jwtProperties.refreshTokenExpirationInMillis());

            // TODO : Redis 도입 시 Refresh Token 저장 로직 추가
            // List<String> key = Collections.singletonList("user:sessions:" + userid);

            return new TokenResult(accessToken, refreshToken);
        } catch (Exception e) {
            throw new BusinessException(CommonErrorCode.INTERNAL_SERVER_ERROR);
        }
    }
}
