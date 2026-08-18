package com.logistics.deliveryservice.application.dto;

import com.logistics.deliveryservice.domain.model.Delivery;
import com.logistics.deliveryservice.domain.model.DeliveryStatus;
import java.util.UUID;

public record DeliveryStatusUpdateResponse(
        UUID deliveryId,
        DeliveryStatus status
) {

    public static DeliveryStatusUpdateResponse from(Delivery delivery) {
        return new DeliveryStatusUpdateResponse(
                delivery.getDeliveryId(),
                delivery.getStatus()
        );
    }
}
