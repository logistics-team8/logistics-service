package com.logistics.hubservice.application.hub.command;

import com.logistics.common.exception.BusinessException;
import com.logistics.hubservice.application.hub.HubErrorCode;
import com.logistics.hubservice.application.hub.dto.HubResponse;
import com.logistics.hubservice.application.hubroute.HubRoutesDeletedEvent;
import com.logistics.hubservice.domain.hub.Hub;
import com.logistics.hubservice.domain.hub.HubRepository;
import com.logistics.hubservice.domain.hubroute.HubRoute;
import com.logistics.hubservice.domain.hubroute.HubRouteRepository;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@Transactional
public class HubCommandService {

    private final HubRepository hubRepository;
    private final HubRouteRepository hubRouteRepository;
    private final ApplicationEventPublisher eventPublisher;

    public HubCommandService(
            HubRepository hubRepository,
            HubRouteRepository hubRouteRepository,
            ApplicationEventPublisher eventPublisher) {
        this.hubRepository = hubRepository;
        this.hubRouteRepository = hubRouteRepository;
        this.eventPublisher = eventPublisher;
    }

    @PreAuthorize("hasRole('MASTER')")
    public HubResponse create(@Valid CreateHubCommand command) {
        Hub hub = Hub.create(command.name(), command.address(), command.latitude(), command.longitude());

        return HubResponse.from(hubRepository.save(hub));
    }

    @PreAuthorize("hasAnyRole('MASTER', 'HUB_MANAGER')")
    @CacheEvict(cacheNames = "hubById", key = "#hubId")
    public HubResponse update(UUID hubId, @Valid UpdateHubCommand command) {
        Hub hub = findActiveHub(hubId);
        hub.update(command.name(), command.address(), command.latitude(), command.longitude());

        return HubResponse.from(hubRepository.save(hub));
    }

    @PreAuthorize("hasRole('MASTER')")
    @CacheEvict(cacheNames = "hubById", key = "#hubId")
    public void delete(UUID hubId, UUID deletedBy) {
        Hub hub = findActiveHubForUpdate(hubId);
        List<HubRoute> connectedHubRoutes =
                hubRouteRepository.findAllByHubIdAndDeletedAtIsNull(hubId);

        connectedHubRoutes.forEach(hubRoute -> hubRoute.delete(deletedBy));
        hubRouteRepository.saveAll(connectedHubRoutes);
        hub.delete(deletedBy);
        hubRepository.save(hub);
        eventPublisher.publishEvent(new HubRoutesDeletedEvent(
                connectedHubRoutes.stream().map(HubRoute::getId).toList()));
    }

    private Hub findActiveHub(UUID hubId) {
        return hubRepository.findByIdAndDeletedAtIsNull(hubId)
                .orElseThrow(() -> new BusinessException(HubErrorCode.HUB_NOT_FOUND));
    }

    private Hub findActiveHubForUpdate(UUID hubId) {
        return hubRepository.findByIdAndDeletedAtIsNullForUpdate(hubId)
                .orElseThrow(() -> new BusinessException(HubErrorCode.HUB_NOT_FOUND));
    }
}
