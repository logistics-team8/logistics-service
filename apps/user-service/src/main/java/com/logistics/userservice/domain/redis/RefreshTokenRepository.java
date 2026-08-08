package com.logistics.userservice.domain.redis;

import java.util.UUID;

public interface RefreshTokenRepository {
    void save(UUID userId, String refreshToken);

    String findByUserId(UUID key);

    void delete(UUID key);
}
