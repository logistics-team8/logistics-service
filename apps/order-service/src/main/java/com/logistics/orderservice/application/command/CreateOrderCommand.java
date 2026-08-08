package com.logistics.orderservice.application.command;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record CreateOrderCommand(
        UUID requesterId,
        UUID receiverCompanyId,
        String requestMessage,
        LocalDateTime requestedDeliveryAt,
        List<CreateOrderItemCommand> items) {

}
