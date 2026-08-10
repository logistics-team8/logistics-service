package com.logistics.orderservice.presentation.dto.response;

import com.logistics.orderservice.domain.model.Order;
import com.logistics.orderservice.domain.model.OrderItem;
import com.logistics.orderservice.domain.model.OrderItemStatus;
import com.logistics.orderservice.domain.model.OrderStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record CancelOrderItemResponse(
        UUID orderId,
        UUID orderItemId,
        OrderStatus orderStatus,
        OrderItemStatus orderItemStatus,
        LocalDateTime canceledAt
) {
    public static CancelOrderItemResponse from(
            Order order,
            OrderItem orderItem
    ) {
        return new CancelOrderItemResponse(
                order.getId(),
                orderItem.getId(),
                order.getStatus(),
                orderItem.getStatus(),
                orderItem.getCanceledAt()
        );
    }
}
