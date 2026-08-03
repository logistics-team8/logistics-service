package com.logistics.userservice.domain;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository {
    User save(User user);

    User saveAndFlush(User user);

    Optional<User> findById(UUID userId);

    Optional<User> findByUsername(String username);

    Optional<User> findByUsernameAndDeletedAtIsNull(String username);

    boolean existsByUsername(String username);

    boolean existsBySlackId(String slackId);

    void deleteAllInBatch();
}
