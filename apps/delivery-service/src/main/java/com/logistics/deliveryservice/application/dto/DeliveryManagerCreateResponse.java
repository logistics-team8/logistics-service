package com.logistics.deliveryservice.application.dto;

import com.logistics.deliveryservice.domain.model.DeliveryManager;
import com.logistics.deliveryservice.domain.model.DeliveryManagerType;
import java.util.UUID;


public record DeliveryManagerCreateResponse(
        UUID userId,
        DeliveryManagerType managerType,
        UUID hubId
) {

    public static DeliveryManagerCreateResponse from(DeliveryManager deliveryManager) {
        return new DeliveryManagerCreateResponse(
                deliveryManager.getUserId(),
                deliveryManager.getManagerType(),
                deliveryManager.getHubId()
        );
    }
}
