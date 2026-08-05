package com.logistics.hubservice.infrastructure.persistence.hub;

import com.logistics.hubservice.domain.hub.Hub;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataHubRepository extends JpaRepository<Hub, UUID> {

    Optional<Hub> findByIdAndDeletedAtIsNull(UUID id);

    List<Hub> findAllByDeletedAtIsNullOrderByCreatedAtDesc();
}
