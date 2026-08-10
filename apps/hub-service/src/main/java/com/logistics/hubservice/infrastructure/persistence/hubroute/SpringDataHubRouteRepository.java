package com.logistics.hubservice.infrastructure.persistence.hubroute;

import com.logistics.hubservice.domain.hubroute.HubRoute;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataHubRouteRepository extends JpaRepository<HubRoute, UUID> {

    Optional<HubRoute> findByIdAndDeletedAtIsNull(UUID id);

    boolean existsBySourceHubIdAndDestinationHubIdAndDeletedAtIsNull(
            UUID sourceHubId, UUID destinationHubId);
}
