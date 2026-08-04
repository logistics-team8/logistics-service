package com.logistics.orderservice.presentation.dto.response;

import com.logistics.orderservice.domain.model.Order;
import com.logistics.orderservice.domain.model.OrderStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record OrderDetailResponse(
        UUID orderId,
        String orderNumber,
        UUID requesterId,
        UUID receiverCompanyId,
        OrderStatus status,
        String requestMessage,
        LocalDateTime requestedDeliveryAt,
        List<OrderItemResponse> items,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static OrderDetailResponse from(Order order) {
        return new OrderDetailResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getRequesterId(),
                order.getReceiverCompanyId(),
                order.getStatus(),
                order.getRequestMessage(),
                order.getRequestedDeliveryAt(),
                order.getOrderItems().stream().map(OrderItemResponse::from).toList(),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }
}
