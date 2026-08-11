package com.logistics.orderservice.domain.model;


public enum OrderFailureReason {
    STOCK_DECREASE_FAILED,
    STOCK_RESTORE_FAILED,
    DELIVERY_CREATE_FAILED,
    STOCK_COMPENSATION_FAILED
}
