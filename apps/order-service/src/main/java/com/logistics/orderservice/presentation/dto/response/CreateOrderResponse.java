package com.logistics.orderservice.presentation.dto.response;

import com.logistics.orderservice.domain.model.Order;
import com.logistics.orderservice.domain.model.OrderStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record CreateOrderResponse(
        UUID orderId,
        String orderNumber,
        OrderStatus status,
        LocalDateTime createdAt
) {
    public static CreateOrderResponse from(Order order){
        return new CreateOrderResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getStatus(),
                order.getCreatedAt()
        );
    }
}
