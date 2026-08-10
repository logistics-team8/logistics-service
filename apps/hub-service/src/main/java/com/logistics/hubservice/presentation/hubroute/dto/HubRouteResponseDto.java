package com.logistics.hubservice.presentation.hubroute.dto;

import com.logistics.hubservice.application.hubroute.dto.HubRouteResponse;
import java.time.LocalDateTime;
import java.util.UUID;

public record HubRouteResponseDto(
        UUID hubRouteId,
        UUID sourceHubId,
        UUID destinationHubId,
        long distanceMeters,
        long durationSeconds,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static HubRouteResponseDto from(HubRouteResponse response) {
        return new HubRouteResponseDto(
                response.hubRouteId(),
                response.sourceHubId(),
                response.destinationHubId(),
                response.distanceMeters(),
                response.durationSeconds(),
                response.createdAt(),
                response.updatedAt()
        );
    }
}
