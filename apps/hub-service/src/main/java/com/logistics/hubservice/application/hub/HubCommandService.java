package com.logistics.hubservice.application.hub;

import com.logistics.common.exception.BusinessException;
import com.logistics.hubservice.domain.hub.Hub;
import com.logistics.hubservice.domain.hub.HubRepository;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@Transactional
public class HubCommandService {

    private final HubRepository hubRepository;

    public HubCommandService(HubRepository hubRepository) {
        this.hubRepository = hubRepository;
    }

    public HubResponse create(@Valid CreateHubCommand command) {
        Hub hub = Hub.create(command.name(), command.address(), command.latitude(), command.longitude());

        return HubResponse.from(hubRepository.save(hub));
    }

    public HubResponse update(UUID hubId, @Valid UpdateHubCommand command) {
        Hub hub = findActiveHub(hubId);
        hub.update(command.name(), command.address(), command.latitude(), command.longitude());

        return HubResponse.from(hubRepository.save(hub));
    }

    public void delete(UUID hubId, UUID deletedBy) {
        Hub hub = findActiveHub(hubId);
        hub.delete(deletedBy);
        hubRepository.save(hub);
    }

    private Hub findActiveHub(UUID hubId) {
        return hubRepository.findByIdAndDeletedAtIsNull(hubId)
                .orElseThrow(() -> new BusinessException(HubErrorCode.HUB_NOT_FOUND));
    }
}
