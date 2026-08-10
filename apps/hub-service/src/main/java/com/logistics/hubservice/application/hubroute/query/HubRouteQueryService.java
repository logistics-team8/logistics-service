package com.logistics.hubservice.application.hubroute.query;

import com.logistics.common.exception.BusinessException;
import com.logistics.hubservice.application.hub.HubErrorCode;
import com.logistics.hubservice.application.hubroute.dto.HubRouteResponse;
import com.logistics.hubservice.domain.hubroute.HubRouteRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class HubRouteQueryService {

    private final HubRouteRepository hubRouteRepository;

    @PreAuthorize("isAuthenticated()")
    @Cacheable(cacheNames = "hubRouteById")
    public HubRouteResponse getOne(UUID hubRouteId) {
        return hubRouteRepository.findByIdAndDeletedAtIsNull(hubRouteId)
                .map(HubRouteResponse::from)
                .orElseThrow(() -> new BusinessException(HubErrorCode.HUB_ROUTE_NOT_FOUND));
    }
}
