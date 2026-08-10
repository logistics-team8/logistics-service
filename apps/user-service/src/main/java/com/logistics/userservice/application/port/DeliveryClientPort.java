package com.logistics.userservice.application.port;

import com.logistics.userservice.infrastructure.client.delivery.DeliveryManagerType;

import java.util.UUID;

public interface DeliveryClientPort {
    void createDeliveryManager(
            UUID userId,
            DeliveryManagerType managedType,
            UUID hubId);
}
