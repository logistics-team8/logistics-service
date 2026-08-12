package com.logistics.deliveryservice.presentation.dto;

import com.logistics.deliveryservice.domain.model.DeliveryManagerType;
import java.util.UUID;

public record DeliveryManagerUpdateRequest(
        DeliveryManagerType managerType,
        UUID hubId
) {

    public boolean hasNoUpdateFields() {
        return managerType == null && hubId == null;
    }
}
