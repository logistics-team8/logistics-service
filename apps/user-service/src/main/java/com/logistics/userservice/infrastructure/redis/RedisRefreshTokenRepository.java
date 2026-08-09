package com.logistics.userservice.infrastructure.redis;

import com.logistics.userservice.domain.redis.RefreshTokenRepository;
import com.logistics.userservice.infrastructure.security.JwtProperties;
import java.time.Duration;
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
    public void save(UUID userId, String refreshToken) {
        redisTemplate
                .opsForValue()
                .set(
                        generateKey(userId),
                        refreshToken,
                        Duration.ofMillis(jwtProperties.refreshTokenExpirationInMillis()));
    }

    @Override
    public Optional<String> findByUserId(UUID userId) {
        return Optional.ofNullable(redisTemplate.opsForValue().get(generateKey(userId)));
    }

    @Override
    public void delete(UUID userId) {
        redisTemplate.delete(generateKey(userId));
    }

    private String generateKey(UUID userId) {
        return REDIS_KEY + userId;
    }
}
