package com.logistics.userservice.infrastructure.redis;

import com.logistics.userservice.domain.redis.RefreshTokenRepository;
import com.logistics.userservice.infrastructure.config.JwtProperties;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RedisRefreshTokenRepository implements RefreshTokenRepository {
    private final StringRedisTemplate redisTemplate;
    private final JwtProperties jwtProperties;
    private static final String REDIS_KEY = "user:refresh:";

    @Override
    public void save(UUID userId, UUID sessionId, String refreshToken) {
        redisTemplate
                .opsForValue()
                .set(
                        generateKey(userId, sessionId),
                        refreshToken,
                        jwtProperties.refreshTokenExpiration());
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
