package com.logistics.hubservice.infrastructure.persistence.hubroute;

import com.logistics.hubservice.domain.hubroute.HubRoute;
import com.logistics.hubservice.domain.hubroute.HubRouteRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class HubRouteJpaRepositoryAdapter implements HubRouteRepository {

    private final SpringDataHubRouteRepository repository;

    @Override
    public HubRoute save(HubRoute hubRoute) {
        return repository.save(hubRoute);
    }

    @Override
    public boolean existsBySourceHubIdAndDestinationHubIdAndDeletedAtIsNull(
            UUID sourceHubId, UUID destinationHubId) {
        return repository.existsBySourceHubIdAndDestinationHubIdAndDeletedAtIsNull(
                sourceHubId, destinationHubId);
    }
}
