package com.logistics.deliveryservice.infrastructure.client.hub.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Hub Service가 반환하는 업체 담당자와 허브 간 배송 계획 계약이다.
 */
public record HubDeliveryPlanResponse(
        UUID companyDeliveryManagerId,
        List<RouteResponse> routes
) {

    public record RouteResponse(
            Integer sequence,
            UUID departureHubId,
            UUID arrivalHubId,
            BigDecimal estimatedDistanceKm,
            Integer estimatedDurationMinutes,
            UUID hubDeliveryManagerId
    ) {
    }
}
