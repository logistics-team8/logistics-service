package com.logistics.userservice.infrastructure.redis;

import com.logistics.userservice.domain.redis.RoleCacheRepository;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RedisRoleCacheRepository implements RoleCacheRepository {
    private final StringRedisTemplate redisTemplate;
    private static final String REDIS_KEY = "user:affiliationType:";
    private static final Duration TTL = Duration.ofMinutes(15);

    @Override
    public void save(UUID userId, String role) {
        redisTemplate.opsForValue().set(generateKey(userId), role, TTL);
    }

    @Override
    public void delete(UUID userId) {
        redisTemplate.delete(generateKey(userId));
    }

    @Override
    public Optional<String> findByUserId(UUID userId) {
        return Optional.ofNullable(redisTemplate.opsForValue().get(generateKey(userId)));
    }

    private String generateKey(UUID userId) {
        return REDIS_KEY + userId;
    }
}
