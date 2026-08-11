package com.logistics.deliveryservice.domain.model;

/**
 * 배송 전체 과정의 현재 진행 상태를 나타낸다.
 */
public enum DeliveryStatus {
    HUB_WAITING,
    HUB_MOVING,
    DEST_HUB_ARRIVED,
    COMPANY_MOVING,
    COMPLETED,
    CANCELED
}
