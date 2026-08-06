package com.logistics.orderservice.presentation.dto.request;

import com.logistics.orderservice.application.command.CreateOrderCommand;
import com.logistics.orderservice.application.command.CreateOrderItemCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record CreateOrderRequest(

        @NotNull
        UUID receiverCompanyId,

        @Size(
                max = 500,
                message = "요청사항은 500자를 초과할 수 없습니다."
        )
        String requestMessage,

        @NotNull(message = "희망 납품 일시는 필수입니다.")
        @Future(message = "희망 납품 일시는 현재로부터 최소 1일 이후여야 합니다.")
        LocalDateTime requestedDeliveryAt,

        @NotEmpty(message = "주문상품은 1개 이상이어야 합니다.")
        List<@NotNull(message = "주문상품 정보는 필수입니다.")
                @Valid CreateOrderItemRequest> items
) {

    public CreateOrderCommand toCommand(UUID requesterId) {
       List<CreateOrderItemCommand> commandItems =
               items.stream()
                       .map(CreateOrderItemRequest::toCommand)
                       .toList();

       return new CreateOrderCommand(
               requesterId,
               receiverCompanyId,
               requestMessage,
               requestedDeliveryAt,
               commandItems
       );
    }
}
