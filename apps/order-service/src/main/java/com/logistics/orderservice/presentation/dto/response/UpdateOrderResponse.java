package com.logistics.orderservice.presentation.dto.response;



import com.logistics.orderservice.domain.model.Order;

import java.time.LocalDateTime;
import java.util.UUID;

public record UpdateOrderResponse(
        UUID orderId,
        String orderNumber,
        String requestMessage,
        LocalDateTime requestDeliverAt,
        LocalDateTime updatedAt,
        UUID updateBy
) {
    public static UpdateOrderResponse from(Order order) {
       return new UpdateOrderResponse(
               order.getId(),
               order.getOrderNumber(),
               order.getRequestMessage(),
               order.getRequestedDeliveryAt(),
               order.getUpdatedAt(),
               order.getUpdatedBy()
       );
    }
}
