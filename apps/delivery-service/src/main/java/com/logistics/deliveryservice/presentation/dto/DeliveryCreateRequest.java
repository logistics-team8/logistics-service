package com.logistics.deliveryservice.presentation.dto;

import com.logistics.deliveryservice.application.command.DeliveryCreateCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * Order Service가 전달하는 배송 생성 요청을 검증하고 Application Command로 변환한다.
 */
public record DeliveryCreateRequest(
        @NotNull(message = "주문 ID는 필수입니다.")
        UUID orderId,

        @NotNull(message = "주문 요청자 ID는 필수입니다.")
        UUID requesterId,

        @NotNull(message = "출발 허브 ID는 필수입니다.")
        UUID departureHubId,

        @NotNull(message = "도착 허브 ID는 필수입니다.")
        UUID arrivalHubId,

        @NotBlank(message = "배송 주소는 필수입니다.")
        String deliveryAddress,

        @NotBlank(message = "수령인 이름은 필수입니다.")
        String receiverName,

        String receiverSlackId
) {

    public DeliveryCreateCommand toCommand() {
        return new DeliveryCreateCommand(
                orderId,
                requesterId,
                departureHubId,
                arrivalHubId,
                deliveryAddress,
                receiverName,
                receiverSlackId
        );
    }
}
