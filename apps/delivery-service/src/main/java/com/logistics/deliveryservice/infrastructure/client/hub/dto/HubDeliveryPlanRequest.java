package com.logistics.deliveryservice.infrastructure.client.hub.dto;

import java.util.UUID;

/**
 * Hub Service에 전체 배송 경로 계획을 요청하는 내부 계약이다.
 */
public record HubDeliveryPlanRequest(
        UUID orderId,
        UUID departureHubId,
        UUID arrivalHubId
) {
}
