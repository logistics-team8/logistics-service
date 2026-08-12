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
        LocalDateTime createdAt,
        String message
) {
    public static CreateOrderResponse created(Order order){
        return new CreateOrderResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getStatus(),
                order.getCreatedAt(),
                "주문이 생성되었습니다."
        );
    }

    public static CreateOrderResponse existing(Order order){
        return new CreateOrderResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getStatus(),
                order.getCreatedAt(),
                "기존 주문이 존재합니다."
        );
    }

    public static CreateOrderResponse from(
            Order order
    ) {
        return created(order);
    }
}
