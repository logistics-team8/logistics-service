package com.logistics.userservice.infrastructure;

import com.logistics.userservice.domain.Role;
import com.logistics.userservice.domain.User;
import com.logistics.userservice.domain.UserRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserJpaRepository extends UserRepository, JpaRepository<User, UUID> {
    Optional<User> findByUsername(String username);

    Optional<User> findByUsernameAndDeletedAtIsNull(String username);

    List<User> findByUsernameOrSlackId(String username, String slackId);

    @Query("SELECT u.role FROM User u WHERE u.id = :userId AND u.deletedAt IS NULL")
    Optional<Role> findRoleById(@Param("userId") UUID userId);

    @Query("SELECT u.slackId FROM User u WHERE u.id = :userId AND u.deletedAt IS NULL")
    Optional<String> findSlackIdById(@Param("userId") UUID userId);
}
