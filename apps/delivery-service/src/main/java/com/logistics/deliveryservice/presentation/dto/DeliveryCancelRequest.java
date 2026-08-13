package com.logistics.deliveryservice.presentation.dto;

/**
 * Order Service가 전달하는 주문 취소 기반 배송 처리 요청이다.
 */
public record DeliveryCancelRequest(
        String cancelReason
) {
}
