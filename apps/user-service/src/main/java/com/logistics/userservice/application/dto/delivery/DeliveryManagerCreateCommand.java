package com.logistics.userservice.application.dto.delivery;

import com.logistics.userservice.domain.RequestedRole;
import java.util.UUID;

public record DeliveryManagerCreateCommand(UUID userId, UUID hubId, RequestedRole managerType) {
    public static DeliveryManagerCreateCommand of(
            UUID userId, UUID hubId, RequestedRole managerType) {
        return new DeliveryManagerCreateCommand(userId, hubId, managerType);
    }
}
