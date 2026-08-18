package com.logistics.deliveryservice.application.dto;

import com.logistics.deliveryservice.domain.model.DeliveryRouteHistory;
import com.logistics.deliveryservice.domain.model.RouteStatus;
import java.util.UUID;

public record DeliveryRouteStatusUpdateResponse(
        UUID routeId,
        RouteStatus status
) {

    public static DeliveryRouteStatusUpdateResponse from(DeliveryRouteHistory routeHistory) {
        return new DeliveryRouteStatusUpdateResponse(
                routeHistory.getRouteId(),
                routeHistory.getStatus()
        );
    }
}
