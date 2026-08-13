package com.logistics.deliveryservice.application.dto;

import com.logistics.deliveryservice.domain.model.DeliveryManager;
import com.logistics.deliveryservice.domain.model.DeliveryManagerType;
import java.util.UUID;

public record DeliveryManagerUpdateResponse(
        UUID userId,
        DeliveryManagerType managerType,
        UUID hubId,
        Integer sequenceNumber
) {

    public static DeliveryManagerUpdateResponse from(DeliveryManager deliveryManager) {
        return new DeliveryManagerUpdateResponse(
                deliveryManager.getUserId(),
                deliveryManager.getManagerType(),
                deliveryManager.getHubId(),
                deliveryManager.getDeliverySequence()
        );
    }
}
