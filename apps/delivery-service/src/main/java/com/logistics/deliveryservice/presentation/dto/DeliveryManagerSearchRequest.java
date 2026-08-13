package com.logistics.deliveryservice.presentation.dto;

import com.logistics.deliveryservice.domain.model.DeliveryManagerType;

import java.util.UUID;

public record DeliveryManagerSearchRequest(
        UUID hubId,
        DeliveryManagerType managerType
) {
}
