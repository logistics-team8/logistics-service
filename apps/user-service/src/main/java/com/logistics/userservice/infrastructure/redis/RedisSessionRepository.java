package com.logistics.userservice.infrastructure.redis;

import com.logistics.userservice.domain.redis.SessionRepository;
import com.logistics.userservice.infrastructure.config.JwtProperties;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RedisSessionRepository implements SessionRepository {
    private static final String REDIS_KEY = "user:sessions:";
    private final RedisScript<Void> loginRedisScript;
    private final StringRedisTemplate redisTemplate;
    private final JwtProperties jwtProperties;

    /**
     * 세션 저장
     *
     * @param userId 사용자 UUID PK
     * @param sessionId 세션 ID
     * @param size 세션 크기
     */
    @Override
    public void save(UUID userId, UUID sessionId, int size) {
        Duration expiration = jwtProperties.refreshTokenExpiration();

        // Instant는 절대시각이라 타임존에 영향 받지 않음
        Instant now = Instant.now();
        Instant expiresAt = now.plus(expiration);

        redisTemplate.execute(
                loginRedisScript,
                List.of(generateKey(userId)),
                sessionId.toString(),
                String.valueOf(now.toEpochMilli()),
                String.valueOf(expiresAt.toEpochMilli()),
                String.valueOf(size),
                String.valueOf(expiration.toSeconds()));
    }

    /**
     * 만료 상태 검증 로직
     *
     * @param userId
     * @param sessionId
     * @return
     */
    @Override
    public boolean exists(UUID userId, UUID sessionId) {
        Double expiresAt =
                redisTemplate.opsForZSet().score(generateKey(userId), sessionId.toString());

        return expiresAt != null // 존재 유무
                && expiresAt > Instant.now().toEpochMilli(); // 만료 유무
    }

    /**
     * 세션 삭제
     *
     * @param userId
     * @param sessionId
     */
    @Override
    public void delete(UUID userId, UUID sessionId) {
        redisTemplate.opsForZSet().remove(generateKey(userId), sessionId.toString());
    }

    /**
     * 전체 삭제
     *
     * @param userId
     */
    @Override
    public void deleteAll(UUID userId) {
        redisTemplate.delete(generateKey(userId));
    }

    /**
     * Redis Key 생성
     *
     * @param userId
     * @return REDIS_KEY
     */
    private String generateKey(UUID userId) {
        return REDIS_KEY + userId;
    }
}
