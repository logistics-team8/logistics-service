package com.logistics.userservice.domain.redis;

import java.util.Optional;
import java.util.UUID;

public interface RoleCacheRepository {
    void save(UUID userId, String role);

    void delete(UUID userId);

    Optional<String> findByUserId(UUID userId);
}
