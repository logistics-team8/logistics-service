package com.logistics.deliveryservice.domain.model;

/**
 * 허브 간 개별 배송 경로의 현재 진행 상태를 나타낸다.
 */
public enum RouteStatus {
    WAITING,
    MOVING,
    ARRIVED
}
