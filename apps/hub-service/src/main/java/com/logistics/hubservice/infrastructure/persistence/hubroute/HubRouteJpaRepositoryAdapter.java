package com.logistics.hubservice.infrastructure.persistence.hubroute;

import com.logistics.hubservice.domain.hubroute.HubRoute;
import com.logistics.hubservice.domain.hubroute.HubRouteRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    public Optional<HubRoute> findByIdAndDeletedAtIsNull(UUID id) {
        return repository.findByIdAndDeletedAtIsNull(id);
    }

    @Override
    public List<HubRoute> findAllByDeletedAtIsNull() {
        return repository.findAllByDeletedAtIsNull();
    }

    @Override
    public Page<HubRoute> search(UUID sourceHubId, UUID destinationHubId, Pageable pageable) {
        return repository.search(sourceHubId, destinationHubId, pageable);
    }

    @Override
    public boolean existsBySourceHubIdAndDestinationHubIdAndDeletedAtIsNull(
            UUID sourceHubId, UUID destinationHubId) {
        return repository.existsBySourceHubIdAndDestinationHubIdAndDeletedAtIsNull(
                sourceHubId, destinationHubId);
    }
}
