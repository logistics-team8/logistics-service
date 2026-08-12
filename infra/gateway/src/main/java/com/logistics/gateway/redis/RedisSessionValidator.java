package com.logistics.gateway.redis;

import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class RedisSessionValidator {
    private static final String REDIS_KEY = "user:sessions:";
    private final ReactiveStringRedisTemplate redisTemplate;

    /**
     * 세션 존재 여부 검증
     *
     * @param userId
     * @param sessionId
     * @return
     */
    public Mono<Boolean> exists(UUID userId, UUID sessionId) {
        return redisTemplate
                .opsForZSet()
                .score(generateKey(userId), sessionId.toString())
                .map(expiresAt -> expiresAt.longValue() > Instant.now().toEpochMilli())
                .defaultIfEmpty(false);
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
