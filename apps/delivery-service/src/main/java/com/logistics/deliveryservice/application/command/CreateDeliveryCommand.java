package com.logistics.deliveryservice.application.command;

import java.util.UUID;

/**
 * 주문 기반 배송 생성에 필요한 불변 요청 정보를 Application 계층으로 전달한다.
 */
public record CreateDeliveryCommand(
        UUID orderId,
        UUID requesterId,
        UUID departureHubId,
        UUID arrivalHubId,
        String deliveryAddress,
        String receiverName,
        String receiverSlackId
) {
}
