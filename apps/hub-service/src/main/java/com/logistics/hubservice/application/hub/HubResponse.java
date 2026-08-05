package com.logistics.hubservice.application.hub;

import com.logistics.hubservice.domain.hub.Hub;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record HubResponse(
        UUID hubId,
        String name,
        String address,
        BigDecimal latitude,
        BigDecimal longitude,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static HubResponse from(Hub hub) {
        return new HubResponse(
                hub.getId(),
                hub.getName(),
                hub.getAddress(),
                hub.getLatitude(),
                hub.getLongitude(),
                hub.getCreatedAt(),
                hub.getUpdatedAt()
        );
    }
}
