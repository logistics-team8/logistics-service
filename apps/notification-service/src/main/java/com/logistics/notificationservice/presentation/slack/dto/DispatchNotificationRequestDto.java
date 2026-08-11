package com.logistics.notificationservice.presentation.slack.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;


public record DispatchNotificationRequestDto(

        UUID orderId,
        String orderNumber,

        String requesterName,
        String requesterSlackId,

        List<ProductInfo> products,

        String requestMessage,
        LocalDateTime requestedDeliveryAt,

        String departureHub,
        List<String> transitHubs,
        String destination,

        UUID recipientUserId,
        String deliveryManagerName,
        String deliveryManagerSlackId
) {

    public record ProductInfo(
            String productName,
            Integer quantity
    ) {
    }
}