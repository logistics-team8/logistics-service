package com.logistics.userservice.application.port;

import com.logistics.userservice.domain.RequestedRole;
import java.util.UUID;

public interface DeliveryClientPort {
    void createDeliveryManager(UUID userId, UUID hubId, RequestedRole managerType);
}
