package com.logistics.deliveryservice.domain.exception;

import com.logistics.common.error.ErrorCode;
import org.springframework.http.HttpStatus;

/**
 * Delivery CRUD v1에서 사용하는 서비스 전용 오류 코드다.
 */
public enum DeliveryErrorCode implements ErrorCode {

    DELIVERY_NOT_FOUND(
            "DEL_001",
            HttpStatus.NOT_FOUND,
            "배송을 찾을 수 없습니다."
    ),
    ROUTE_NOT_FOUND(
            "DEL_002",
            HttpStatus.NOT_FOUND,
            "배송 경로를 찾을 수 없습니다."
    ),
    DUPLICATE_ORDER_DELIVERY(
            "DEL_003",
            HttpStatus.CONFLICT,
            "동일한 주문에 대한 배송 생성 요청이 충돌했습니다."
    ),
    INVALID_DELIVERY_STATUS_TRANSITION(
            "DEL_004",
            HttpStatus.CONFLICT,
            "현재 배송 상태에서는 요청한 상태로 변경할 수 없습니다."
    ),
    INVALID_ROUTE_STATUS_TRANSITION(
            "DEL_005",
            HttpStatus.CONFLICT,
            "현재 배송 경로 상태에서는 요청한 상태로 변경할 수 없습니다."
    ),
    CANCEL_OR_DELETE_NOT_ALLOWED(
            "DEL_006",
            HttpStatus.CONFLICT,
            "현재 배송 상태에서는 취소하거나 삭제할 수 없습니다."
    ),
    INVALID_HUB_DELIVERY_PLAN(
            "DEL_007",
            HttpStatus.BAD_REQUEST,
            "Hub 배송 계획 데이터가 유효하지 않습니다."
    ),
    HUB_DELIVERY_PLAN_UNAVAILABLE(
            "DEL_008",
            HttpStatus.SERVICE_UNAVAILABLE,
            "Hub 배송 계획 서비스를 사용할 수 없습니다."
    );

    private final String code;
    private final HttpStatus status;
    private final String message;

    DeliveryErrorCode(String code, HttpStatus status, String message) {
        this.code = code;
        this.status = status;
        this.message = message;
    }

    @Override
    public String code() {
        return code;
    }

    @Override
    public HttpStatus status() {
        return status;
    }

    @Override
    public String message() {
        return message;
    }
}
