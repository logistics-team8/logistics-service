package com.logistics.deliveryservice.presentation.dto;

import com.logistics.deliveryservice.domain.model.DeliveryStatus;
import java.util.UUID;


public record DeliverySearchRequest(
        DeliveryStatus status,
        UUID orderId
) {
}
