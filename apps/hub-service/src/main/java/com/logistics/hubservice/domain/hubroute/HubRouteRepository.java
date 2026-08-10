package com.logistics.hubservice.domain.hubroute;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface HubRouteRepository {

    HubRoute save(HubRoute hubRoute);

    List<HubRoute> saveAll(List<HubRoute> hubRoutes);

    Optional<HubRoute> findByIdAndDeletedAtIsNull(UUID id);

    List<HubRoute> findAllByHubIdAndDeletedAtIsNull(UUID hubId);

    Page<HubRoute> search(UUID sourceHubId, UUID destinationHubId, Pageable pageable);

    boolean existsBySourceHubIdAndDestinationHubIdAndDeletedAtIsNull(
            UUID sourceHubId, UUID destinationHubId);
}
