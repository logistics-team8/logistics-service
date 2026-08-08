package com.logistics.orderservice.presentation.dto.response;

import com.logistics.orderservice.domain.model.Order;

import java.time.LocalDateTime;
import java.util.UUID;

public record DeleteOrderResponse(
        UUID orderId,
        LocalDateTime deletedAt,
        UUID deletedBy
) {
    public static DeleteOrderResponse from(Order order) {
        return new DeleteOrderResponse(
                order.getId(),
                order.getDeletedAt(),
                order.getDeletedBy()
        );
    }
}
