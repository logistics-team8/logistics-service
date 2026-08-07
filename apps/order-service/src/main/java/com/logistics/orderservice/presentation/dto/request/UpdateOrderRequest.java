package com.logistics.orderservice.presentation.dto.request;

import com.logistics.orderservice.application.command.UpdateOrderCommand;
import com.logistics.orderservice.presentation.validation.AtLeastDaysInFuture;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record UpdateOrderRequest(
        @Size(
                max = 500,
                message = "요청사항은 500자를 초과할 수 없습니다."
        )
        String requestMessage,

        //희망 납품 일시는 현재로부터 최소 1일 이후로 지정한다.
        @AtLeastDaysInFuture(days = 1)
        LocalDateTime requestedDeliveryAt
) {
    public UpdateOrderCommand toCommand() {
        return new UpdateOrderCommand(
                requestMessage,
                requestedDeliveryAt
        );
    }
}
