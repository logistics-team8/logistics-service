package com.logistics.orderservice.domain.model;

public enum OrderStatus {
    // 주문 데이터 생성 완료, 재고 차감 전
    PENDING,
    // 재고 차감 성공 → 주문 확정
    CONFIRMED,
    // 배송 생성 성공 → 주문 생성 프로세스 완료
    DELIVERY_CREATED,
    CANCELED,
    FAILED
}
