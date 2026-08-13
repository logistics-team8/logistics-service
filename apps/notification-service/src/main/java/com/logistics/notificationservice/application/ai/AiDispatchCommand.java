package com.logistics.notificationservice.application.ai;

import com.logistics.notificationservice.presentation.slack.dto.DispatchNotificationRequestDto;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
public record AiDispatchCommand(

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

        String deliveryManagerName,
        String deliveryManagerSlackId,

        LocalTime workStartTime,
        LocalTime workEndTime
) {

    private static final LocalTime WORK_START_TIME =
            LocalTime.of(9, 0);

    private static final LocalTime WORK_END_TIME =
            LocalTime.of(18, 0);

    public static AiDispatchCommand from(
            DispatchNotificationRequestDto request
    ) {

        List<ProductInfo> products =
                request.products().stream()
                        .map(product ->
                                new ProductInfo(
                                        product.productName(),
                                        product.quantity()
                                )
                        )
                        .toList();

        return new AiDispatchCommand(
                request.orderId(),
                request.orderNumber(),
                request.requesterName(),
                request.requesterSlackId(),
                products,
                request.requestMessage(),
                request.requestedDeliveryAt(),
                request.departureHub(),
                request.transitHubs(),
                request.destination(),
                request.deliveryManagerName(),
                request.deliveryManagerSlackId(),
                WORK_START_TIME,
                WORK_END_TIME
        );
    }

    public record ProductInfo(
            String productName,
            Integer quantity
    ) {
    }
}