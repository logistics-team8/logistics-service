package com.logistics.userservice.infrastructure.client.delivery;

import com.logistics.userservice.domain.RequestedRole;
import java.util.UUID;

public record CreateDeliveryManagerRequest(UUID userId, UUID hubId, RequestedRole managerType) {
    public static CreateDeliveryManagerRequest of(
            UUID userId, UUID hubId, RequestedRole managerType) {
        return new CreateDeliveryManagerRequest(userId, hubId, managerType);
    }
}
