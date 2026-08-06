package com.logistics.userservice.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository {
    User save(User user);

    User saveAndFlush(User user);

    Optional<User> findById(UUID userId);

    Optional<User> findByIdAndDeletedAtIsNull(UUID userId);

    Optional<User> findByUsername(String username);

    Optional<User> findByUsernameAndDeletedAtIsNull(String username);

    List<User> findByUsernameOrSlackId(String username, String slackId);

    void deleteAllInBatch();

    Optional<Role> findRoleByIdDeletedAtIsNull(UUID userId);

    Optional<String> findSlackIdByIdDeletedAtIsNull(UUID userId);
}
