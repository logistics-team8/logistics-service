package com.logistics.orderservice.error;

import com.logistics.common.error.ErrorCode;
import org.springframework.http.HttpStatus;

public enum OrderErrorCode implements ErrorCode {

    ORDER_NOT_FOUND(
            "ORD_001",
            HttpStatus.NOT_FOUND,
            "주문을 찾을 수 없습니다."
    ),

    ORDER_ITEM_REQUIRED(
            "ORD_002",
            HttpStatus.BAD_REQUEST,
            "주문상품은 1개 이상이어야 합니다."
    ),

    INVALID_ORDER_QUANTITY(
            "ORD_003",
            HttpStatus.BAD_REQUEST,
            "주문 수량은 1개 이상이어야 합니다."
    ),

    INVALID_ORDER_STATUS(
            "ORD_004",
            HttpStatus.CONFLICT,
            "현재 주문 상태에서는 해당 작업을 수행할 수 없습니다."
    ),

    DUPLICATE_ORDER_NUMBER(
            "ORD_005",
            HttpStatus.CONFLICT,
            "이미 사용 중인 주문번호입니다."
    ),

    PRODUCT_NOT_ASSIGNED(
            "ORD_006",
            HttpStatus.CONFLICT,
            "주문상품 정보가 연결되지 않았습니다."
    ),

    DELIVERY_NOT_ASSIGNED(
            "ORD_007",
            HttpStatus.CONFLICT,
            "배송 정보가 연결되지 않았습니다."
    ),

    PRODUCT_ID_REQUIRED(
            "ORD_008",
            HttpStatus.BAD_REQUEST,
            "상품 ID는 필수입니다."
    ),

    DUPLICATE_ORDER_PRODUCT(
            "ORD_009",
            HttpStatus.CONFLICT,
            "동일한 상품을 중복으로 주문할 수 없습니다."
    ),

    ORDER_NOT_UPDATABLE(
            "ORD_010",
            HttpStatus.CONFLICT,
            "현재 주문 상태에서는 주문을 수정할 수 없습니다."
    ),

    ORDER_NOT_DELETABLE(
            "ORD_011",
            HttpStatus.CONFLICT,
            "현재 주문 상태에서는 주문을 삭제할 수 없습니다."
    ),

    ORDER_ALREADY_DELETED(
            "ORD_012",
            HttpStatus.CONFLICT,
            "이미 삭제된 주문입니다."
    ),

    ORDER_UPDATE_FORBIDDEN(
            "ORD_013",
            HttpStatus.FORBIDDEN,
            "해당 주문을 수정할 권한이 없습니다."
    ),

    ORDER_DELETE_FORBIDDEN(
            "ORD_014",
            HttpStatus.FORBIDDEN,
            "해당 주문을 삭제할 권한이 없습니다."
    ),

    INVALID_REQUESTED_DELIVERY_AT(
            "ORD_015",
            HttpStatus.BAD_REQUEST,
            "희망 납품 일시는 현재 시각으로부터 최소 1일 이후여야 합니다."
    ),

    REQUESTED_DELIVERY_AT_REQUIRED(
            "ORD_016",
            HttpStatus.BAD_REQUEST,
                "희망 납품 일시는 필수입니다."
    ),

    ORDER_NOT_CANCELABLE(
        "ORD_017",
        HttpStatus.CONFLICT,
        "현재 주문 상태에서는 주문을 취소할 수 없습니다."
    ),

    ORDER_ALREADY_CANCELED(
        "ORD_018",
        HttpStatus.CONFLICT,
        "이미 취소된 주문입니다."
    ),

    ORDER_CANCEL_FORBIDDEN(
        "ORD_019",
        HttpStatus.FORBIDDEN,
        "해당 주문을 취소할 권한이 없습니다."
    ),

    ORDER_ITEM_NOT_FOUND(
        "ORD_020",
        HttpStatus.NOT_FOUND,
        "주문상품을 찾을 수 없습니다."
    ),

    ORDER_ITEM_ALREADY_CANCELED(
        "ORD_021",
        HttpStatus.CONFLICT,
        "이미 취소된 주문상품입니다."
    ),

    PRODUCT_NOT_FOUND(
            "ORD_022",
            HttpStatus.NOT_FOUND,
            "주문 상품을 찾을 수 없습니다."
    ),
    DIFFERENT_DEPARTURE_HUB(
            "ORD_023",
            HttpStatus.BAD_REQUEST,
            "하나의 주문에는 동일한 허브에 소속된 상품만 포함할 수 있습니다."
    ),
    ORDER_AUTHENTICATION_REQUIRED(
            "ORD_401",
            HttpStatus.UNAUTHORIZED,
            "로그인이 필요합니다."
    ),

    ORDER_ACCESS_DENIED(
            "ORDER_403",
            HttpStatus.FORBIDDEN,
            "해당 주문에 접근할 권한이 없습니다."
    ),
    DELIVERY_CANCEL_NOT_SUPPORTED(
            "ORD_024",
            HttpStatus.CONFLICT,
            "배송이 생성된 주문은 현재 취소할 수 없습니다."
    ),
    DELIVERY_CREATE_FAILED(
            "ORD_025",
            HttpStatus.BAD_GATEWAY,
            "배송 생성에 실패했습니다."
    ),

    DELIVERY_STATUS_CHECK_FAILED(
            "ORD_026",
            HttpStatus.SERVICE_UNAVAILABLE,
            "배송 생성 결과를 확인할 수 없습니다."
    ),

    DELIVERY_REQUEST_CONFLICT(
            "ORD_027",
            HttpStatus.CONFLICT,
            "기존 배송 정보가 주문의 배송 요청 정보와 일치하지 않습니다."
    ),
    // 재고 차감 실패
    STOCK_DECREASE_FAILED(
            "ORD_028",
            HttpStatus.BAD_GATEWAY,
                "재고 차감 요청에 실패했습니다."
    ),

    // 배송 실패 후 재고 복원까지 실패
    STOCK_RESTORE_FAILED(
            "ORD_029",
            HttpStatus.BAD_GATEWAY,
                "배송 생성 실패 후 재고 복원에 실패했습니다."
    ),

    ORDER_CANCEL_STOCK_RESTORE_FAILED(
            "ORD_030",
            HttpStatus.BAD_GATEWAY,
                "주문 취소 중 재고 복원에 실패했습니다."
    ),
    STOCK_DECREASE_UNKNOWN(
            "ORD_031",
            HttpStatus.SERVICE_UNAVAILABLE,
            "재고 차감 결과를 확인할 수 없습니다."
    ),
    STOCK_RESTORE_UNKNOWN(
            "ORD_032",
            HttpStatus.SERVICE_UNAVAILABLE,
            "재고 복원 결과를 확인할 수 없습니다."
    ),
    ORDER_CANCEL_STOCK_RESTORE_UNKNOWN(
            "ORD_033",
            HttpStatus.SERVICE_UNAVAILABLE,
            "주문 취소 중 재고 복원 결과를 확인할 수 없습니다."
    );


    private final String code;
    private final HttpStatus status;
    private final String message;

    OrderErrorCode(
            String code,
            HttpStatus status,
            String message
    ) {
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
