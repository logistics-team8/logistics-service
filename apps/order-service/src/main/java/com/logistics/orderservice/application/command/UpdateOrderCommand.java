package com.logistics.orderservice.application.command;

import java.time.LocalDateTime;

public record UpdateOrderCommand(
        String requestMessage,
        LocalDateTime requestedDeliveryAt
) {
}
