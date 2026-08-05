package com.logistics.orderservice.application.service;

import com.logistics.common.exception.BusinessException;
import com.logistics.orderservice.domain.model.Order;
import com.logistics.orderservice.domain.repository.OrderRepository;
import com.logistics.orderservice.error.OrderErrorCode;
import com.logistics.orderservice.presentation.dto.response.OrderDetailResponse;
import com.logistics.orderservice.presentation.dto.response.OrderSummaryResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderQueryServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderQueryService orderQueryService;

    private UUID orderId;
    private UUID requesterId;
    private UUID otherUserId;
    private Order order;

    @BeforeEach
    void setUp() {
        orderId = UUID.randomUUID();
        requesterId = UUID.randomUUID();
        otherUserId = UUID.randomUUID();

        order = Order.create(
                "ORD-20260805-123456",
                requesterId,
                UUID.randomUUID(),
                "테스트 주문입니다.",
                LocalDateTime.of(2026, 8, 10, 10, 0)
        );
    }

    @Test
    @DisplayName("마스터는 주문 ID로 모든 주문을 단건 조회할 수 있다")
    void getOrder_master_success() {
        // given
        given(orderRepository.findByIdAndDeletedAtIsNull(orderId))
                .willReturn(Optional.of(order));

        // when
        OrderDetailResponse response = orderQueryService.getOrder(
                orderId,
                UUID.randomUUID(),
                "ROLE_MASTER"
        );

        // then
        assertThat(response).isNotNull();
        assertThat(response.orderNumber())
                .isEqualTo("ORD-20260805-123456");

        verify(orderRepository)
                .findByIdAndDeletedAtIsNull(orderId);

        verify(orderRepository, never())
                .findByIdAndRequesterIdAndDeletedAtIsNull(
                        orderId,
                        requesterId
                );
    }

    @Test
    @DisplayName("마스터가 존재하지 않는 주문을 조회하면 예외가 발생한다")
    void getOrder_master_notFound() {
        // given
        given(orderRepository.findByIdAndDeletedAtIsNull(orderId))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() ->
                orderQueryService.getOrder(
                        orderId,
                        UUID.randomUUID(),
                        "ROLE_MASTER"
                )
        )
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> {
                    BusinessException businessException =
                            (BusinessException) exception;

                    assertThat(businessException.getErrorCode())
                            .isEqualTo(OrderErrorCode.ORDER_NOT_FOUND);
                });
    }

    @Test
    @DisplayName("마스터가 아닌 로그인 사용자는 자신이 생성한 주문을 조회할 수 있다")
    void getOrder_requester_success() {
        // given
        given(
                orderRepository.findByIdAndRequesterIdAndDeletedAtIsNull(
                        orderId,
                        requesterId
                )
        ).willReturn(Optional.of(order));

        // when
        OrderDetailResponse response = orderQueryService.getOrder(
                orderId,
                requesterId,
                "ROLE_COMPANY_MANAGER"
        );

        // then
        assertThat(response).isNotNull();
        assertThat(response.orderNumber())
                .isEqualTo("ORD-20260805-123456");

        verify(orderRepository)
                .findByIdAndRequesterIdAndDeletedAtIsNull(
                        orderId,
                        requesterId
                );

        verify(orderRepository, never())
                .findByIdAndDeletedAtIsNull(orderId);
    }

    @Test
    @DisplayName("다른 사용자의 주문은 조회할 수 없다")
    void getOrder_otherUser_notFound() {
        // given
        given(
                orderRepository.findByIdAndRequesterIdAndDeletedAtIsNull(
                        orderId,
                        otherUserId
                )
        ).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() ->
                orderQueryService.getOrder(
                        orderId,
                        otherUserId,
                        "ROLE_DELIVERY_MANAGER"
                )
        )
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> {
                    BusinessException businessException =
                            (BusinessException) exception;

                    assertThat(businessException.getErrorCode())
                            .isEqualTo(OrderErrorCode.ORDER_NOT_FOUND);
                });
    }

    @Test
    @DisplayName("마스터는 삭제되지 않은 전체 주문 목록을 조회한다")
    void getOrders_master_success() {
        // given
        Pageable pageable = PageRequest.of(0, 10);

        Page<Order> orderPage = new PageImpl<>(
                List.of(order),
                pageable,
                1
        );

        given(orderRepository.findAllByDeletedAtIsNull(pageable))
                .willReturn(orderPage);

        // when
        Page<OrderSummaryResponse> response =
                orderQueryService.getOrders(
                        pageable,
                        UUID.randomUUID(),
                        "ROLE_MASTER"
                );

        // then
        assertThat(response.getTotalElements()).isEqualTo(1);
        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().getFirst().orderNumber())
                .isEqualTo("ORD-20260805-123456");

        verify(orderRepository)
                .findAllByDeletedAtIsNull(pageable);
    }

    @Test
    @DisplayName("마스터가 아닌 로그인 사용자는 자신이 생성한 주문 목록만 조회한다")
    void getOrders_requester_success() {
        // given
        Pageable pageable = PageRequest.of(0, 10);

        Page<Order> orderPage = new PageImpl<>(
                List.of(order),
                pageable,
                1
        );

        given(
                orderRepository.findAllByRequesterIdAndDeletedAtIsNull(
                        requesterId,
                        pageable
                )
        ).willReturn(orderPage);

        // when
        Page<OrderSummaryResponse> response =
                orderQueryService.getOrders(
                        pageable,
                        requesterId,
                        "ROLE_COMPANY_MANAGER"
                );

        // then
        assertThat(response.getTotalElements()).isEqualTo(1);
        assertThat(response.getContent()).hasSize(1);

        verify(orderRepository)
                .findAllByRequesterIdAndDeletedAtIsNull(
                        requesterId,
                        pageable
                );

        verify(orderRepository, never())
                .findAllByDeletedAtIsNull(pageable);
    }
}