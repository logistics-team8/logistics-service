package com.logistics.deliveryservice.application.dto;

import com.logistics.deliveryservice.domain.model.DeliveryRouteHistory;
import com.logistics.deliveryservice.domain.model.RouteStatus;
import java.math.BigDecimal;
import java.util.UUID;

// 배송 경로 이력 목록의 한 건 응답
public record DeliveryRouteHistoryResponse(
        UUID routeId,
        Integer sequence,
        UUID departureHubId,
        UUID arrivalHubId,
        BigDecimal estimatedDistance,
        Integer estimatedDuration,
        RouteStatus status,
        UUID hubDeliveryManagerId
) {

    public static DeliveryRouteHistoryResponse from(DeliveryRouteHistory routeHistory) {
        return new DeliveryRouteHistoryResponse(
                routeHistory.getRouteId(),
                routeHistory.getSequence(),
                routeHistory.getDepartureHubId(),
                routeHistory.getArrivalHubId(),
                routeHistory.getEstimatedDistanceKm(),
                routeHistory.getEstimatedDurationMinutes(),
                routeHistory.getStatus(),
                routeHistory.getHubDeliveryManagerId()
        );
    }
}
