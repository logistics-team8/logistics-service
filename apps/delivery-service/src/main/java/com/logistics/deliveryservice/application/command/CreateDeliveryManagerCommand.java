package com.logistics.deliveryservice.application.command;

import com.logistics.deliveryservice.domain.model.DeliveryManagerType;
import java.util.UUID;


public record CreateDeliveryManagerCommand(
        UUID userId,
        DeliveryManagerType managerType,
        UUID hubId
) {
}
