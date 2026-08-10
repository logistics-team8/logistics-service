package com.logistics.userservice.infrastructure.client.delivery;

import java.util.UUID;

public record CreateDeliveryManagerRequest(
        UUID userId,
        DeliveryManagerType managerType,
        UUID hubId
) {
    public static CreateDeliveryManagerRequest of(UUID userId, DeliveryManagerType managerType, UUID hubId) {
        return new CreateDeliveryManagerRequest(userId, managerType, hubId);
    }
}
