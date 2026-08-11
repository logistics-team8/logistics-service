package com.logistics.deliveryservice.domain.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class DeliveryErrorCodeTest {

    @Test
    void definesDeliveryCrudV1ErrorCodes() {
        assertThat(DeliveryErrorCode.values())
                .extracting(
                        DeliveryErrorCode::code,
                        DeliveryErrorCode::status,
                        DeliveryErrorCode::message
                )
                .containsExactly(
                        tuple("DEL_001", HttpStatus.NOT_FOUND, "배송을 찾을 수 없습니다."),
                        tuple("DEL_002", HttpStatus.NOT_FOUND, "배송 경로를 찾을 수 없습니다."),
                        tuple(
                                "DEL_003",
                                HttpStatus.CONFLICT,
                                "동일한 주문에 대한 배송 생성 요청이 충돌했습니다."
                        ),
                        tuple(
                                "DEL_004",
                                HttpStatus.CONFLICT,
                                "현재 배송 상태에서는 요청한 상태로 변경할 수 없습니다."
                        ),
                        tuple(
                                "DEL_005",
                                HttpStatus.CONFLICT,
                                "현재 배송 경로 상태에서는 요청한 상태로 변경할 수 없습니다."
                        ),
                        tuple(
                                "DEL_006",
                                HttpStatus.CONFLICT,
                                "현재 배송 상태에서는 취소하거나 삭제할 수 없습니다."
                        ),
                        tuple(
                                "DEL_007",
                                HttpStatus.BAD_REQUEST,
                                "Hub 배송 계획 데이터가 유효하지 않습니다."
                        ),
                        tuple(
                                "DEL_008",
                                HttpStatus.SERVICE_UNAVAILABLE,
                                "Hub 배송 계획 서비스를 사용할 수 없습니다."
                        )
                );
    }

    @Test
    void exposesErrorCodeAndCauseThroughDeliveryException() {
        IllegalStateException cause = new IllegalStateException("Hub 호출 실패");

        DeliveryException exception = new DeliveryException(
                DeliveryErrorCode.HUB_DELIVERY_PLAN_UNAVAILABLE,
                cause
        );

        assertThat(exception.getErrorCode())
                .isSameAs(DeliveryErrorCode.HUB_DELIVERY_PLAN_UNAVAILABLE);
        assertThat(exception)
                .hasMessage("Hub 배송 계획 서비스를 사용할 수 없습니다.")
                .hasCause(cause);
    }
}
