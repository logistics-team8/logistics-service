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
    ),
    DELIVERY_MANAGER_NOT_FOUND(
            "DEL_009",
            HttpStatus.NOT_FOUND,
            "배송 담당자를 찾을 수 없습니다."
    ),
    DUPLICATE_DELIVERY_MANAGER(
            "DEL_010",
            HttpStatus.CONFLICT,
            "이미 등록된 배송 담당자입니다."
    ),
    INVALID_DELIVERY_MANAGER_USER(
            "DEL_011",
            HttpStatus.BAD_REQUEST,
            "사용자가 없거나 배송 담당자 역할이 아닙니다."
    ),
    INVALID_DELIVERY_MANAGER_HUB(
            "DEL_012",
            HttpStatus.BAD_REQUEST,
            "허브가 없거나 담당자 유형과 허브 정보가 일치하지 않습니다."
    ),
    DELIVERY_MANAGER_GROUP_FULL(
            "DEL_013",
            HttpStatus.CONFLICT,
            "배정 그룹의 활성 배송 담당자는 최대 10명입니다."
    ),
    DELIVERY_MANAGER_UNAVAILABLE(
            "DEL_014",
            HttpStatus.CONFLICT,
            "배정 가능한 배송 담당자가 없습니다."
    ),
    DELIVERY_MANAGER_IN_USE(
            "DEL_015",
            HttpStatus.CONFLICT,
            "진행 중인 배송이 있어 배송 담당자를 변경하거나 삭제할 수 없습니다."
    ),
    INVALID_DELIVERY_MANAGER_CHANGE(
            "DEL_016",
            HttpStatus.CONFLICT,
            "배송 담당자 수정 또는 복구 요청이 유효하지 않습니다."
    ),
    USER_SERVICE_UNAVAILABLE(
            "DEL_017",
            HttpStatus.SERVICE_UNAVAILABLE,
            "User Service를 사용할 수 없습니다."
    ),
    HUB_SERVICE_UNAVAILABLE(
            "DEL_018",
            HttpStatus.SERVICE_UNAVAILABLE,
            "Hub Service를 사용할 수 없습니다."
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
