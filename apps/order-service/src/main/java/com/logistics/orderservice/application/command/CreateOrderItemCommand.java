package com.logistics.orderservice.application.command;

import java.util.UUID;

public record CreateOrderItemCommand(
        UUID productId,
        Integer quantity
) {

}
