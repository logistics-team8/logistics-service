package com.logistics.orderservice.application.service.command;

import com.logistics.common.exception.BusinessException;
import com.logistics.orderservice.application.exception.DeliveryCreateException;
import com.logistics.orderservice.application.exception.DeliveryLookupException;
import com.logistics.orderservice.application.exception.DeliveryStatusUnknownException;
import com.logistics.orderservice.application.port.DeliveryPort;
import com.logistics.orderservice.error.OrderErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DeliveryRequestServiceTest {
    @Mock DeliveryPort deliveryPort;
    @InjectMocks DeliveryRequestService service;

    private DeliveryPort.CreateDeliveryCommand command;
    private DeliveryPort.DeliveryInfo delivery;

    @BeforeEach
    void setUp() {
        command = new DeliveryPort.CreateDeliveryCommand(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "서울", "홍길동", "hong.slack");
        delivery = new DeliveryPort.DeliveryInfo(
                UUID.randomUUID(), command.orderId(), command.requesterId(), "PENDING",
                command.departureHubId(), command.arrivalHubId(), command.deliveryAddress(),
                command.receiverName(), command.receiverSlackId());
    }

    @Test
    @DisplayName("첫 배송 생성 요청이 성공하면 조회하지 않는다")
    void request_firstSuccess() {
        given(deliveryPort.createDelivery(command)).willReturn(delivery);
        assertThat(service.requestDelivery(command)).contains(delivery);
        verify(deliveryPort, never()).findDeliveryByOrderId(command.orderId());
    }

    @Test
    @DisplayName("첫 생성 실패 후 동일한 기존 배송을 반환한다")
    void request_existingDelivery() {
        given(deliveryPort.createDelivery(command)).willThrow(new DeliveryCreateException("failed"));
        given(deliveryPort.findDeliveryByOrderId(command.orderId())).willReturn(Optional.of(delivery));
        assertThat(service.requestDelivery(command)).contains(delivery);
        verify(deliveryPort).createDelivery(command);
    }

    @Test
    @DisplayName("배송이 없으면 한 번만 생성 재시도한다")
    void request_retry() {
        given(deliveryPort.createDelivery(command))
                .willThrow(new DeliveryCreateException("failed")).willReturn(delivery);
        given(deliveryPort.findDeliveryByOrderId(command.orderId())).willReturn(Optional.empty());
        assertThat(service.requestDelivery(command)).contains(delivery);
        verify(deliveryPort, times(2)).createDelivery(command);
    }

    @Test
    @DisplayName("배송 조회 실패는 상태 미확인 예외로 변환한다")
    void request_lookupUnknown() {
        given(deliveryPort.createDelivery(command)).willThrow(new DeliveryCreateException("failed"));
        given(deliveryPort.findDeliveryByOrderId(command.orderId()))
                .willThrow(new DeliveryLookupException("lookup failed"));
        assertThatThrownBy(() -> service.requestDelivery(command))
                .isInstanceOf(DeliveryStatusUnknownException.class)
                .hasCauseInstanceOf(DeliveryLookupException.class);
    }

    @Test
    @DisplayName("기존 배송 payload가 다르면 충돌 예외가 발생한다")
    void request_conflict() {
        DeliveryPort.DeliveryInfo conflict = new DeliveryPort.DeliveryInfo(
                UUID.randomUUID(), command.orderId(), command.requesterId(), "PENDING",
                command.departureHubId(), command.arrivalHubId(), "부산", command.receiverName(), command.receiverSlackId());
        given(deliveryPort.createDelivery(command)).willThrow(new DeliveryCreateException("failed"));
        given(deliveryPort.findDeliveryByOrderId(command.orderId())).willReturn(Optional.of(conflict));

        assertThatThrownBy(() -> service.requestDelivery(command))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(OrderErrorCode.DELIVERY_REQUEST_CONFLICT));
    }
}
