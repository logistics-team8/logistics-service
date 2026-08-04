package com.logistics.orderservice.presentation.dto.response;

import com.logistics.orderservice.domain.model.OrderItem;
import com.logistics.orderservice.domain.model.OrderItemStatus;

import java.util.UUID;

public record OrderItemResponse(
        UUID orderItemId,
        UUID productId,
        String productName,
        Integer quantity,
        OrderItemStatus status,
        UUID deliveryId

) {
    public static OrderItemResponse from(OrderItem orderItem) {
        return new OrderItemResponse(
                orderItem.getId(),
                orderItem.getProductId(),
                orderItem.getProductName(),
                orderItem.getQuantity(),
                orderItem.getStatus(),
                orderItem.getDeliveryId()
        );
    }
}
