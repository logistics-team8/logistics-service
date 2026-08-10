package com.logistics.hubservice.domain.hubroute;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface HubRouteRepository {

    HubRoute save(HubRoute hubRoute);

    Optional<HubRoute> findByIdAndDeletedAtIsNull(UUID id);

    Page<HubRoute> search(UUID sourceHubId, UUID destinationHubId, Pageable pageable);

    boolean existsBySourceHubIdAndDestinationHubIdAndDeletedAtIsNull(
            UUID sourceHubId, UUID destinationHubId);
}
