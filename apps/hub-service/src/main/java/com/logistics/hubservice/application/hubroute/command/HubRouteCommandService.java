package com.logistics.hubservice.application.hubroute.command;

import com.logistics.common.exception.BusinessException;
import com.logistics.hubservice.application.hub.HubErrorCode;
import com.logistics.hubservice.application.hubroute.dto.HubRouteResponse;
import com.logistics.hubservice.domain.hub.HubRepository;
import com.logistics.hubservice.domain.hubroute.HubRoute;
import com.logistics.hubservice.domain.hubroute.HubRouteRepository;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@Transactional
@RequiredArgsConstructor
public class HubRouteCommandService {

    private final HubRepository hubRepository;
    private final HubRouteRepository hubRouteRepository;

    @PreAuthorize("hasRole('MASTER')")
    public HubRouteResponse create(@Valid CreateHubRouteCommand command) {
        validateDifferentHubs(command.sourceHubId(), command.destinationHubId());
        validateActiveHub(command.sourceHubId());
        validateActiveHub(command.destinationHubId());
        validateNoActiveDuplicate(command.sourceHubId(), command.destinationHubId());

        HubRoute hubRoute = HubRoute.create(
                command.sourceHubId(),
                command.destinationHubId(),
                command.distanceMeters(),
                command.durationSeconds()
        );

        return HubRouteResponse.from(hubRouteRepository.save(hubRoute));
    }

    @PreAuthorize("hasRole('MASTER')")
    @Caching(evict = {
            @CacheEvict(cacheNames = "hubRouteById", key = "#hubRouteId"),
            @CacheEvict(cacheNames = "hubRoutePath", allEntries = true)
    })
    public HubRouteResponse update(UUID hubRouteId, @Valid UpdateHubRouteCommand command) {
        HubRoute hubRoute = hubRouteRepository.findByIdAndDeletedAtIsNull(hubRouteId)
                .orElseThrow(() -> new BusinessException(HubErrorCode.HUB_ROUTE_NOT_FOUND));
        hubRoute.update(command.distanceMeters(), command.durationSeconds());

        return HubRouteResponse.from(hubRouteRepository.save(hubRoute));
    }

    private void validateDifferentHubs(UUID sourceHubId, UUID destinationHubId) {
        if (sourceHubId.equals(destinationHubId)) {
            throw new BusinessException(HubErrorCode.HUB_ROUTE_SAME_HUB);
        }
    }

    private void validateActiveHub(UUID hubId) {
        if (hubRepository.findByIdAndDeletedAtIsNull(hubId).isEmpty()) {
            throw new BusinessException(HubErrorCode.HUB_NOT_FOUND);
        }
    }

    private void validateNoActiveDuplicate(UUID sourceHubId, UUID destinationHubId) {
        if (hubRouteRepository.existsBySourceHubIdAndDestinationHubIdAndDeletedAtIsNull(
                sourceHubId, destinationHubId)) {
            throw new BusinessException(HubErrorCode.HUB_ROUTE_DUPLICATE);
        }
    }
}
