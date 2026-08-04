package com.logistics.orderservice.presentation.dto.response;

import com.logistics.orderservice.domain.model.Order;
import com.logistics.orderservice.domain.model.OrderStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record OrderSummaryResponse(
        UUID orderId,
        String orderNumber,
        OrderStatus status,
        UUID receiverCompanyId,
        LocalDateTime requestedDeliveryAt,
        LocalDateTime createdAt
) {
    public static OrderSummaryResponse from(Order order) {
        return new OrderSummaryResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getStatus(),
                order.getReceiverCompanyId(),
                order.getRequestedDeliveryAt(),
                order.getCreatedAt()
        );
    }
}
