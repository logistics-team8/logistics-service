package com.logistics.hubservice.presentation.hubroute.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateDeliveryPlanRequest(
        @NotNull(message = "주문 ID는 필수입니다.")
        UUID orderId,
        @NotNull(message = "출발 허브 ID는 필수입니다.")
        UUID departureHubId,
        @NotNull(message = "도착 허브 ID는 필수입니다.")
        UUID arrivalHubId
) {
}
