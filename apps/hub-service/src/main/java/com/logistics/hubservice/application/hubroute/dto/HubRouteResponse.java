package com.logistics.hubservice.application.hubroute.dto;

import com.logistics.hubservice.domain.hubroute.HubRoute;
import java.time.LocalDateTime;
import java.util.UUID;

public record HubRouteResponse(
        UUID hubRouteId,
        UUID sourceHubId,
        UUID destinationHubId,
        long distanceMeters,
        long durationSeconds,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static HubRouteResponse from(HubRoute hubRoute) {
        return new HubRouteResponse(
                hubRoute.getId(),
                hubRoute.getSourceHubId(),
                hubRoute.getDestinationHubId(),
                hubRoute.getDistanceMeters(),
                hubRoute.getDurationSeconds(),
                hubRoute.getCreatedAt(),
                hubRoute.getUpdatedAt()
        );
    }
}
