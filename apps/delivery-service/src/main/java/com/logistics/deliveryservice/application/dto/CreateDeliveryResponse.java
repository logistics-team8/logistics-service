package com.logistics.deliveryservice.application.dto;

import com.logistics.deliveryservice.domain.model.Delivery;
import com.logistics.deliveryservice.domain.model.DeliveryRouteHistory;
import com.logistics.deliveryservice.domain.model.DeliveryStatus;
import com.logistics.deliveryservice.domain.model.RouteStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * 생성되었거나 멱등 조회된 Delivery Aggregate를 내부 API 응답으로 변환한다.
 */
public record CreateDeliveryResponse(
        UUID deliveryId,
        UUID orderId,
        UUID requesterId,
        DeliveryStatus status,
        UUID departureHubId,
        UUID arrivalHubId,
        String deliveryAddress,
        String receiverName,
        String receiverSlackId,
        UUID companyDeliveryManagerId,
        LocalDateTime completedAt,
        LocalDateTime createdAt,
        List<RouteResponse> routes
) {

    public static CreateDeliveryResponse from(Delivery delivery) {
        // JPA 컬렉션의 조회 순서에 의존하지 않고 Route를 이동 순서대로 응답한다.
        List<RouteResponse> routes = delivery.getRouteHistories().stream()
                .sorted(Comparator.comparingInt(DeliveryRouteHistory::getSequence))
                .map(RouteResponse::from)
                .toList();

        return new CreateDeliveryResponse(
                delivery.getDeliveryId(),
                delivery.getOrderId(),
                delivery.getRequesterId(),
                delivery.getStatus(),
                delivery.getDepartureHubId(),
                delivery.getArrivalHubId(),
                delivery.getDeliveryAddress(),
                delivery.getReceiverName(),
                delivery.getReceiverSlackId(),
                delivery.getCompanyDeliveryManagerId(),
                delivery.getCompletedAt(),
                delivery.getCreatedAt(),
                routes
        );
    }

    public record RouteResponse(
            UUID routeId,
            Integer sequence,
            UUID departureHubId,
            UUID arrivalHubId,
            BigDecimal estimatedDistanceKm,
            Integer estimatedDurationMinutes,
            RouteStatus status,
            UUID hubDeliveryManagerId
    ) {

        private static RouteResponse from(DeliveryRouteHistory route) {
            return new RouteResponse(
                    route.getRouteId(),
                    route.getSequence(),
                    route.getDepartureHubId(),
                    route.getArrivalHubId(),
                    route.getEstimatedDistanceKm(),
                    route.getEstimatedDurationMinutes(),
                    route.getStatus(),
                    route.getHubDeliveryManagerId()
            );
        }
    }
}
