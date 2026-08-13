package com.logistics.userservice.application;

import com.logistics.common.exception.BusinessException;
import com.logistics.userservice.application.dto.auth.UserLoginCommand;
import com.logistics.userservice.application.token.TokenClaims;
import com.logistics.userservice.application.token.TokenPayload;
import com.logistics.userservice.application.token.TokenProvider;
import com.logistics.userservice.application.token.TokenResult;
import com.logistics.userservice.domain.Role;
import com.logistics.userservice.domain.User;
import com.logistics.userservice.domain.UserRepository;
import com.logistics.userservice.domain.redis.RefreshTokenRepository;
import com.logistics.userservice.domain.redis.RoleCacheRepository;
import com.logistics.userservice.domain.redis.SessionRepository;
import com.logistics.userservice.error.AuthErrorCode;
import com.logistics.userservice.error.ClientErrorCode;
import com.logistics.userservice.error.UserErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {
    private final RefreshTokenRepository refreshTokenRepository;
    private final RoleCacheRepository roleCacheRepository;
    private final SessionRepository sessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final TokenProvider tokenProvider;

    /**
     * 로그인 요청이 들어왔을 시 User 검증 후 액세스 토큰과 리프레시 토큰 발급
     *
     * @param command Login 정보가 담긴 DTO
     * @return Token값이 담긴 TokenResult DTO 반환
     */
    public TokenResult login(UserLoginCommand command) {
        User user =
                userRepository
                        .findByUsernameAndDeletedAtIsNull(command.username())
                        .orElseThrow(() -> new BusinessException(AuthErrorCode.INVALID_LOGIN));

        if (!passwordEncoder.matches(command.password(), user.getPassword())) {
            throw new BusinessException(AuthErrorCode.INVALID_LOGIN);
        }

        // TODO : 사용자 상태 검증 로직 (PENDING, REJECTED)
        user.validateActive();

        TokenClaims tokenClaims =
                new TokenClaims(user.getId(), user.getHubId(), user.getCompanyId());

        return createLoginResponse(tokenClaims, UUID.randomUUID(), user.getRole());
    }

    /**
     * 로그아웃 요청이 들어왔을 시 Redis에서 Refresh Token과 Role 캐싱 삭제
     *
     * @param accessToken
     */
    public void logout(String accessToken) {
        try {
            TokenPayload tokenPayload = tokenProvider.getAllClaimsFromAccessToken(accessToken);
            deleteUserDataFromRedis(tokenPayload.userId(), tokenPayload.sessionId());
        } catch (ExpiredJwtException e) {
            Claims claims = e.getClaims();

            log.info("[EXPIRED] 만료된 Access Token으로 로그아웃 ");
            UUID userId = UUID.fromString(claims.getSubject());
            UUID sessionId = UUID.fromString(claims.get("sessionId", String.class));

            deleteUserDataFromRedis(userId, sessionId);

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
            UUID sessionId = tokenPayload.sessionId(); // 기존 사용자가 재발급 하므로 세션 ID 유지

            try {
                if (!sessionRepository.exists(userId, sessionId)) {
                    throw new BusinessException(AuthErrorCode.TOKEN_INVALID);
                }
            } catch (DataAccessException e) {
                log.error("[ERROR] 인증 세션 조회 실패 userId = {}", userId, e);
                throw new BusinessException(ClientErrorCode.SERVICE_UNAVAILABLE);
            }

            User user =
                    userRepository
                            .findByIdAndDeletedAtIsNull(userId)
                            .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

            TokenClaims tokenClaims =
                    new TokenClaims(user.getId(), user.getHubId(), user.getCompanyId());

            return rotateRefreshToken(tokenClaims, sessionId, user.getRole(), refreshToken);

        } catch (ExpiredJwtException e) {
            throw new BusinessException(AuthErrorCode.TOKEN_EXPIRED);

        } catch (JwtException | IllegalArgumentException e) {
            throw new BusinessException(AuthErrorCode.TOKEN_INVALID);
        }
    }

    // ============================== Helper Method ====================================
    /**
     * 액세스 토큰, 리프래시 토큰 생성 후 Redis에 Session, Refresh Token을 저장한 뒤 TokenResult 반환
     *
     * @param tokenClaims 토큰 클레임 DTO
     * @param sessionId 사용자 식별 세션 ID
     * @param role 사용자 권한
     * @return TokenResult (AccessToken, RefreshToken)
     */
    private TokenResult createLoginResponse(TokenClaims tokenClaims, UUID sessionId, Role role) {
        UUID userId = tokenClaims.userId();

        String accessToken = tokenProvider.generateAccessToken(tokenClaims, sessionId);
        String refreshToken = tokenProvider.generateRefreshToken(tokenClaims, sessionId);

        try {
            // 세션 저장
            sessionRepository.save(tokenClaims.userId(), sessionId, getSessionSize(role));

            // 리프레시 토큰 저장
            refreshTokenRepository.save(userId, sessionId, refreshToken);
        } catch (DataAccessException e) {
            log.error("[ERROR] 인증 세션 저장 실패 userId = {}", userId, e);
            throw new BusinessException(ClientErrorCode.SERVICE_UNAVAILABLE);
        }

        try {
            roleCacheRepository.save(userId, role.name());
        } catch (DataAccessException e) {
            log.warn("[CACHE] Role Cache 저장 실패 userId = {}", userId, e);
        }
        return new TokenResult(accessToken, refreshToken);
    }

    /**
     * 토큰 재발급
     *
     * @param tokenClaims 토큰 클레임 DTO
     * @param sessionId 사용자 식별 세션 ID
     * @param role 사용자 권한
     * @param oldRefreshToken
     * @return TokenResult (AccessToken, RefreshToken)
     */
    private TokenResult rotateRefreshToken(
            TokenClaims tokenClaims, UUID sessionId, Role role, String oldRefreshToken) {

        UUID userId = tokenClaims.userId();

        String newAccessToken = tokenProvider.generateAccessToken(tokenClaims, sessionId);
        String newRefreshToken = tokenProvider.generateRefreshToken(tokenClaims, sessionId);

        try {
            boolean result =
                    refreshTokenRepository.rotate(
                            userId, sessionId, oldRefreshToken, newRefreshToken);

            if (!result) {
                log.warn(
                        "[FAIL] Refresh Token 재발급 실패 userId = {}, sessionId = {}",
                        userId,
                        sessionId);

                throw new BusinessException(AuthErrorCode.TOKEN_INVALID);
            }

            sessionRepository.save(userId, sessionId, getSessionSize(role));

        } catch (DataAccessException e) {
            log.error(
                    "[ERROR] Refresh Token 재발급 실패 userId = {}, sessionId = {}",
                    userId,
                    sessionId,
                    e);

            throw new BusinessException(ClientErrorCode.SERVICE_UNAVAILABLE);
        }

        try {
            roleCacheRepository.save(userId, role.name());
        } catch (DataAccessException e) {
            log.warn("[CACHE] Role Cache 저장 실패 userId = {}", userId, e);
        }

        return new TokenResult(newAccessToken, newRefreshToken);
    }

    /**
     * 권한에 따라 세션 크기 결정
     *
     * @param role
     * @return
     */
    private int getSessionSize(Role role) {
        return Role.DELIVERY_MANAGER == role ? 5 : 1;
    }

    /**
     * Redis에서 User 인증 정보 삭제
     *
     * @param userId
     */
    private void deleteUserDataFromRedis(UUID userId, UUID sessionId) {
        try {
            refreshTokenRepository.delete(userId, sessionId);
            sessionRepository.delete(userId, sessionId);
        } catch (DataAccessException e) {
            log.error("[ERROR] 인증 세션 삭제 실패 userId = {}", userId, e);
            throw new BusinessException(ClientErrorCode.SERVICE_UNAVAILABLE);
        }
        try {
            roleCacheRepository.delete(userId);
        } catch (DataAccessException e) {
            log.warn("[CACHE] Role Cache 삭제 실패 userId = {}", userId, e);
        }
    }
}
