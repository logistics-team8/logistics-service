package com.logistics.userservice.domain.redis;

import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository {
    void save(UUID userId, UUID sessionId, String refreshToken);

    boolean rotate(UUID userId, UUID sessionId, String oldRefreshToken, String newRefreshToken);

    Optional<String> findByUserId(UUID userId, UUID sessionId);

    void delete(UUID userId, UUID sessionId);
}
