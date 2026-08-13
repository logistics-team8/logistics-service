package com.logistics.userservice.infrastructure.redis;

import com.logistics.userservice.domain.redis.RefreshTokenRepository;
import com.logistics.userservice.infrastructure.config.JwtProperties;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RedisRefreshTokenRepository implements RefreshTokenRepository {
    private static final String REDIS_KEY = "user:refresh:";
    private final StringRedisTemplate redisTemplate;
    private final JwtProperties jwtProperties;
    private final RedisScript<Boolean> reissueTokenScript;

    /**
     * 리프레시 토큰 저장
     *
     * @param userId 사용자 PK
     * @param sessionId 세션 ID
     * @param refreshToken 리프레시 토큰 값
     */
    @Override
    public void save(UUID userId, UUID sessionId, String refreshToken) {
        redisTemplate
                .opsForValue()
                .set(
                        generateKey(userId, sessionId),
                        refreshToken,
                        jwtProperties.refreshTokenExpiration());
    }

    /**
     * 리프레시 토큰 재발급
     *
     * @param userId 사용자 PK
     * @param sessionId 세션 ID
     * @param oldRefreshToken 기존 리프레시 토큰 값
     * @param newRefreshToken 새로 생성된 리프레시 토큰 값
     * @return True | False
     */
    @Override
    public boolean rotate(
            UUID userId, UUID sessionId, String oldRefreshToken, String newRefreshToken) {
        Duration expiration = jwtProperties.refreshTokenExpiration();

        return redisTemplate.execute(
                reissueTokenScript,
                List.of(generateKey(userId, sessionId)),
                oldRefreshToken,
                newRefreshToken,
                String.valueOf(expiration.toSeconds()));
    }

    @Override
    public Optional<String> findByUserId(UUID userId, UUID sessionId) {
        return Optional.ofNullable(redisTemplate.opsForValue().get(generateKey(userId, sessionId)));
    }

    @Override
    public void delete(UUID userId, UUID sessionId) {
        redisTemplate.delete(generateKey(userId, sessionId));
    }

    private String generateKey(UUID userId, UUID sessionId) {
        return REDIS_KEY + "{" + userId + "}:" + sessionId;
    }
}
