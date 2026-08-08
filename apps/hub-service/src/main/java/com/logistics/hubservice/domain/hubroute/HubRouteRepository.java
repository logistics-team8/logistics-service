package com.logistics.hubservice.domain.hubroute;

import java.util.UUID;

public interface HubRouteRepository {

    HubRoute save(HubRoute hubRoute);

    boolean existsBySourceHubIdAndDestinationHubIdAndDeletedAtIsNull(
            UUID sourceHubId, UUID destinationHubId);
}
