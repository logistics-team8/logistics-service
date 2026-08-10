package com.logistics.orderservice.presentation.dto.response;

import com.logistics.orderservice.domain.model.Order;
import com.logistics.orderservice.domain.model.OrderStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record CancelOrderResponse(
        UUID orderId,
        String orderNumber,
        OrderStatus status,
        LocalDateTime canceledAt
) {
    public static CancelOrderResponse from(Order order) {
        return new CancelOrderResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getStatus(),
                order.getCanceledAt()
        );
    }
}
