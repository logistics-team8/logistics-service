package com.logistics.deliveryservice.infrastructure.client.hub.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Hub Service가 반환하는 허브 간 이동 경로 계획이다. 담당자 배정은 Delivery가 수행한다.
 */
public record HubDeliveryPlanResponse(
        List<RouteResponse> routes
) {

    public record RouteResponse(
            Integer sequence,
            UUID departureHubId,
            UUID arrivalHubId,
            BigDecimal estimatedDistanceKm,
            Integer estimatedDurationMinutes
    ) {
    }
}
