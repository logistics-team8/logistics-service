package com.logistics.userservice.application;

import com.logistics.common.error.CommonErrorCode;
import com.logistics.common.exception.BusinessException;
import com.logistics.userservice.application.dto.auth.UserLoginCommand;
import com.logistics.userservice.application.token.TokenClaims;
import com.logistics.userservice.application.token.TokenPayload;
import com.logistics.userservice.application.token.TokenProvider;
import com.logistics.userservice.application.token.TokenResult;
import com.logistics.userservice.domain.User;
import com.logistics.userservice.domain.UserRepository;
import com.logistics.userservice.domain.redis.RefreshTokenRepository;
import com.logistics.userservice.domain.redis.RoleCacheRepository;
import com.logistics.userservice.error.AuthErrorCode;
import com.logistics.userservice.error.UserErrorCode;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final TokenProvider tokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final RoleCacheRepository roleCacheRepository;

    /**
     * 로그인 요청이 들어왔을 시 User 검증 후 액세스 토큰과 리프레시 토큰 발급
     *
     * @param command Login 정보가 담긴 DTO
     * @return Token값이 담긴 TokenResult DTO 반환
     */
    @Transactional(readOnly = true)
    public TokenResult login(UserLoginCommand command) {
        User user =
                userRepository
                        .findByUsernameAndDeletedAtIsNull(command.username())
                        .orElseThrow(() -> new BusinessException(AuthErrorCode.INVALID_LOGIN));

        if (!passwordEncoder.matches(command.password(), user.getPassword())) {
            throw new BusinessException(AuthErrorCode.INVALID_LOGIN);
        }

        // TODO : 사용자 상태 검증 로직 (PENDING, REJECTED)
        //        개발 어느정도 진행됐을 시 주석 해제
        // user.validateActive();

        TokenClaims tokenClaims =
                new TokenClaims(user.getId(), user.getHubId(), user.getCompanyId());

        return createAuthResponse(tokenClaims, UUID.randomUUID(), user.getRole().name());
    }

    /**
     * 로그아웃 요청이 들어왔을 시 Redis에서 Refresh Token과 Role 캐싱 삭제
     *
     * @param accessToken
     */
    public void logout(String accessToken) {
        try {
            TokenPayload tokenPayload = tokenProvider.getAllClaimsFromAccessToken(accessToken);
            deleteUserDataFromRedis(tokenPayload.userId());

        } catch (ExpiredJwtException e) {
            log.info("[Expired] 만료된 Access Token으로 로그아웃 ");
            UUID userId = UUID.fromString(e.getClaims().getSubject());
            deleteUserDataFromRedis(userId);

        } catch (JwtException | IllegalArgumentException e) {
            throw new BusinessException(AuthErrorCode.TOKEN_INVALID);
        }
    }

    /**
     * Token 재발급 기존의 Refresh Token을 제거한 뒤 새로운 토큰을 재발급
     *
     * @param refreshToken
     * @return Token값이 담긴 TokenResult DTO 반환
     */
    public TokenResult reissue(String refreshToken) {
        try {
            TokenPayload tokenPayload = tokenProvider.getAllClaimsFromRefreshToken(refreshToken);
            UUID userId = tokenPayload.userId();
            String savedRefreshToken;

            try {
                savedRefreshToken = refreshTokenRepository.findByUserId(userId).orElse(null);
            } catch (DataAccessException e) {
                log.error("[ERROR] Refresh Token 조회 실패 userId = {}", userId, e);
                throw new BusinessException(CommonErrorCode.INTERNAL_SERVER_ERROR);
            }

            validateRefreshToken(refreshToken, savedRefreshToken);

            User user =
                    userRepository
                            .findByIdAndDeletedAtIsNull(userId)
                            .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

            TokenClaims tokenClaims =
                    new TokenClaims(user.getId(), user.getHubId(), user.getCompanyId());

            // 새로운 세션이 아닌 기존 사용자가 재발급 하므로 세션 ID 유지
            UUID sessionId = tokenPayload.sessionId();

            return createAuthResponse(tokenClaims, sessionId, user.getRole().name());

        } catch (ExpiredJwtException e) {
            throw new BusinessException(AuthErrorCode.TOKEN_EXPIRED);

        } catch (JwtException | IllegalArgumentException e) {
            throw new BusinessException(AuthErrorCode.TOKEN_INVALID);
        }
    }

    /**
     * 액세스 토큰, 리프래시 토큰 생성 후 Redis에 Refresh Token을 저장한 뒤 TokenResult 반환
     *
     * @param tokenClaims 토큰 클레임 DTO
     * @param sessionId 사용자 식별 세션 ID
     * @param role 사용자 권한
     * @return TokenResult (AccessToken, RefreshToken)
     */
    private TokenResult createAuthResponse(TokenClaims tokenClaims, UUID sessionId, String role) {
        UUID userId = tokenClaims.userId();

        String accessToken = tokenProvider.generateAccessToken(tokenClaims, sessionId);
        String refreshToken = tokenProvider.generateRefreshToken(tokenClaims, sessionId);

        try {
            refreshTokenRepository.save(userId, refreshToken);
        } catch (DataAccessException e) {
            log.error("[ERROR] Refresh Token 저장 실패 userId = {}", userId, e);
            throw new BusinessException(CommonErrorCode.INTERNAL_SERVER_ERROR);
        }

        try {
            roleCacheRepository.save(userId, role);
        } catch (DataAccessException e) {
            log.warn("[Cache] Role Cache 저장 실패 userId = {}", userId, e);
        }
        return new TokenResult(accessToken, refreshToken);
    }

    /**
     * 리프레시 토큰이 일치하는지 확인
     *
     * @param refreshToken
     * @param savedToken
     */
    private void validateRefreshToken(String refreshToken, String savedToken) {
        if (!StringUtils.hasText(savedToken) || !Objects.equals(refreshToken, savedToken)) {
            throw new BusinessException(AuthErrorCode.TOKEN_INVALID);
        }
    }

    /**
     * Redis에서 User 인증 정보 삭제
     *
     * @param userId
     */
    private void deleteUserDataFromRedis(UUID userId) {
        try {
            refreshTokenRepository.delete(userId);
        } catch (DataAccessException e) {
            log.error("[ERROR] Refresh Token 삭제 실패 userId = {}", userId, e);
            throw new BusinessException(CommonErrorCode.INTERNAL_SERVER_ERROR);
        }
        try {
            roleCacheRepository.delete(userId);
        } catch (DataAccessException e) {
            log.warn("[Cache] Role Cache 삭제 실패 userId = {}", userId, e);
        }
    }
}
