package com.logistics.hubservice.infrastructure.persistence.hubroute;

import com.logistics.hubservice.domain.hubroute.HubRoute;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface SpringDataHubRouteRepository extends JpaRepository<HubRoute, UUID> {

    Optional<HubRoute> findByIdAndDeletedAtIsNull(UUID id);

    @Query("""
            SELECT hr
            FROM HubRoute hr
            WHERE hr.deletedAt IS NULL
              AND (hr.sourceHubId = :hubId OR hr.destinationHubId = :hubId)
            """)
    List<HubRoute> findAllByHubIdAndDeletedAtIsNull(@Param("hubId") UUID hubId);

    @Query("""
            SELECT hr
            FROM HubRoute hr
            WHERE hr.deletedAt IS NULL
              AND (:sourceHubId IS NULL OR hr.sourceHubId = :sourceHubId)
              AND (:destinationHubId IS NULL OR hr.destinationHubId = :destinationHubId)
            """)
    Page<HubRoute> search(
            @Param("sourceHubId") UUID sourceHubId,
            @Param("destinationHubId") UUID destinationHubId,
            Pageable pageable);

    boolean existsBySourceHubIdAndDestinationHubIdAndDeletedAtIsNull(
            UUID sourceHubId, UUID destinationHubId);
}
