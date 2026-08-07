package com.logistics.userservice.domain.redis;

import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository {
    void save(UUID userId, String refreshToken);

    Optional<String> findByUserId(UUID key);

    void delete(UUID key);
}
