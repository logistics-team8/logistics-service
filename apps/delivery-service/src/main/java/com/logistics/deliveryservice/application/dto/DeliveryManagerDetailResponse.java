package com.logistics.deliveryservice.application.dto;

import com.logistics.deliveryservice.domain.model.DeliveryManager;
import com.logistics.deliveryservice.domain.model.DeliveryManagerType;
import java.util.UUID;

public record DeliveryManagerDetailResponse(
        UUID userId,
        DeliveryManagerType managerType,
        UUID hubId,
        Integer sequenceNumber,
        String slackId
) {

    public static DeliveryManagerDetailResponse from(
            DeliveryManager deliveryManager,
            String slackId
    ) {
        return new DeliveryManagerDetailResponse(
                deliveryManager.getUserId(),
                deliveryManager.getManagerType(),
                deliveryManager.getHubId(),
                deliveryManager.getDeliverySequence(),
                slackId
        );
    }
}
