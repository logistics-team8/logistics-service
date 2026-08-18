package com.logistics.deliveryservice.presentation.dto;

import com.logistics.deliveryservice.domain.model.RouteStatus;
import jakarta.validation.constraints.NotNull;

public record DeliveryRouteStatusUpdateRequest(
        @NotNull(message = "배송 경로 상태는 필수입니다.")
        RouteStatus status
) {
}
