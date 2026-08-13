package com.logistics.deliveryservice.application.dto;

import com.logistics.deliveryservice.domain.model.Delivery;
import com.logistics.deliveryservice.domain.model.DeliveryStatus;
import java.util.UUID;

// 배송 한 건의 상세 조회 응답
public record DeliveryDetailResponse(
        UUID deliveryId,
        UUID orderId,
        DeliveryStatus status,
        UUID departureHubId,
        UUID arrivalHubId,
        String deliveryAddress,
        String receiverName,
        String receiverSlackId,
        UUID deliveryManagerId
) {

    public static DeliveryDetailResponse from(Delivery delivery) {
        return new DeliveryDetailResponse(
                delivery.getDeliveryId(),
                delivery.getOrderId(),
                delivery.getStatus(),
                delivery.getDepartureHubId(),
                delivery.getArrivalHubId(),
                delivery.getDeliveryAddress(),
                delivery.getReceiverName(),
                delivery.getReceiverSlackId(),
                delivery.getDeliveryManagerId()
        );
    }
}
