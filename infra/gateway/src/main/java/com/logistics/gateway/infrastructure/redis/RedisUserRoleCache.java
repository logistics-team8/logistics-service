package com.logistics.gateway.infrastructure.redis;

import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class RedisUserRoleCache {
    private final ReactiveStringRedisTemplate redisTemplate;

    private static final String REDIS_KEY = "user:role:";
    private static final Duration TTL = Duration.ofMinutes(30);

    public Mono<String> findByUserId(String userId) {
        return redisTemplate.opsForValue().get(generateKey(userId));
    }

    public Mono<String> save(String userId, String role) {
        return redisTemplate.opsForValue().set(generateKey(userId), role, TTL).thenReturn(role);
    }

    private String generateKey(String userId) {
        return REDIS_KEY + userId;
    }
}
