package com.logistics.deliveryservice.application.dto;

import com.logistics.deliveryservice.domain.model.DeliveryManager;
import com.logistics.deliveryservice.domain.model.DeliveryManagerType;
import java.util.UUID;


public record CreateDeliveryManagerResponse(
        UUID userId,
        DeliveryManagerType managerType,
        UUID hubId,
        Integer sequenceNumber
) {

    public static CreateDeliveryManagerResponse from(DeliveryManager deliveryManager) {
        return new CreateDeliveryManagerResponse(
                deliveryManager.getUserId(),
                deliveryManager.getManagerType(),
                deliveryManager.getHubId(),
                deliveryManager.getDeliverySequence()
        );
    }
}
