package com.logistics.orderservice.application.port;

import java.util.UUID;

public interface DeliveryPort {
    DeliveryInfo createDelivery(CreateDeliveryCommand deliveryCommand);

    record CreateDeliveryCommand(
            UUID orderId,
            UUID requesterId,
            UUID departureHubId,
            UUID arrivalHubId,
            String deliveryAddress,
            String receiverName,
            String receiverSlackId
    ){
    }

    record DeliveryInfo(
            UUID deliveryId,
            UUID orderId,
            String status
    ){
    }


}
