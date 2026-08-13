package com.logistics.deliveryservice.application.dto;

import com.logistics.deliveryservice.domain.model.Delivery;
import com.logistics.deliveryservice.domain.model.DeliveryStatus;
import java.util.UUID;


public record DeliverySearchResponse(
        UUID deliveryId,
        UUID orderId,
        DeliveryStatus status
) {

    public static DeliverySearchResponse from(Delivery delivery) {
        return new DeliverySearchResponse(
                delivery.getDeliveryId(),
                delivery.getOrderId(),
                delivery.getStatus()
        );
    }
}
