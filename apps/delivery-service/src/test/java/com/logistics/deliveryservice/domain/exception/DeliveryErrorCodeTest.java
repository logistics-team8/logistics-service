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
                        ),
                        tuple(
                                "DEL_009",
                                HttpStatus.NOT_FOUND,
                                "배송 담당자를 찾을 수 없습니다."
                        ),
                        tuple(
                                "DEL_010",
                                HttpStatus.CONFLICT,
                                "이미 등록된 배송 담당자입니다."
                        ),
                        tuple(
                                "DEL_011",
                                HttpStatus.BAD_REQUEST,
                                "사용자가 없거나 배송 담당자 역할이 아닙니다."
                        ),
                        tuple(
                                "DEL_012",
                                HttpStatus.BAD_REQUEST,
                                "허브가 없거나 담당자 유형과 허브 정보가 일치하지 않습니다."
                        ),
                        tuple(
                                "DEL_013",
                                HttpStatus.CONFLICT,
                                "배정 그룹의 활성 배송 담당자는 최대 10명입니다."
                        ),
                        tuple(
                                "DEL_014",
                                HttpStatus.CONFLICT,
                                "배정 가능한 배송 담당자가 없습니다."
                        ),
                        tuple(
                                "DEL_015",
                                HttpStatus.CONFLICT,
                                "진행 중인 배송이 있어 배송 담당자를 변경하거나 삭제할 수 없습니다."
                        ),
                        tuple(
                                "DEL_016",
                                HttpStatus.CONFLICT,
                                "배송 담당자 수정 또는 복구 요청이 유효하지 않습니다."
                        ),
                        tuple(
                                "DEL_017",
                                HttpStatus.SERVICE_UNAVAILABLE,
                                "User Service를 사용할 수 없습니다."
                        ),
                        tuple(
                                "DEL_018",
                                HttpStatus.SERVICE_UNAVAILABLE,
                                "Hub Service를 사용할 수 없습니다."
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
