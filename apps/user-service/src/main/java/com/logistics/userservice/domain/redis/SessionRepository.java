package com.logistics.userservice.domain.redis;

import java.util.UUID;

public interface SessionRepository {
    void save(UUID userId, UUID sessionId, int size);

    boolean exists(UUID userId, UUID sessionId);

    void delete(UUID userId, UUID sessionId);

    void deleteAll(UUID userId);
}
