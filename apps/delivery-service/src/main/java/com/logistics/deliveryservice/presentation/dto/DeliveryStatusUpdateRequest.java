package com.logistics.deliveryservice.presentation.dto;

import com.logistics.deliveryservice.domain.model.DeliveryStatus;
import jakarta.validation.constraints.NotNull;

public record DeliveryStatusUpdateRequest(
        @NotNull(message = "배송 상태는 필수입니다.")
        DeliveryStatus status
) {
}
