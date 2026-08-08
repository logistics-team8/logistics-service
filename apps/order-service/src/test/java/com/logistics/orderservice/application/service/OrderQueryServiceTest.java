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
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderQueryServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderQueryService orderQueryService;

    private UUID orderId;
    private String orderNumber;
    private LocalDateTime fixedNow;
    private Order order;

    @BeforeEach
    void setUp() {
        orderId = UUID.randomUUID();
        orderNumber = "ORD-20260805-123456";

        fixedNow = LocalDateTime.of(
                2026,
                8,
                5,
                10,
                0
        );

        order = Order.create(
                orderNumber,
                UUID.randomUUID(),
                UUID.randomUUID(),
                "테스트 주문입니다.",
                fixedNow.plusDays(3),
                fixedNow
        );
    }

    @Test
    @DisplayName("주문 ID로 삭제되지 않은 주문을 단건 조회한다")
    void getOrder_success() {
        // given
        given(orderRepository.findByIdAndDeletedAtIsNull(orderId))
                .willReturn(Optional.of(order));

        // when
        OrderDetailResponse response =
                orderQueryService.getOrder(orderId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.orderNumber())
                .isEqualTo("ORD-20260805-123456");

        verify(orderRepository)
                .findByIdAndDeletedAtIsNull(orderId);
    }

    @Test
    @DisplayName("존재하지 않거나 삭제된 주문을 단건 조회하면 예외가 발생한다")
    void getOrder_notFound() {
        // given
        given(orderRepository.findByIdAndDeletedAtIsNull(orderId))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() ->
                orderQueryService.getOrder(orderId)
        )
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> {
                    BusinessException businessException =
                            (BusinessException) exception;

                    assertThat(businessException.getErrorCode())
                            .isEqualTo(OrderErrorCode.ORDER_NOT_FOUND);
                });

        verify(orderRepository)
                .findByIdAndDeletedAtIsNull(orderId);
    }

    @Test
    @DisplayName("삭제되지 않은 주문 목록을 페이징하여 조회한다")
    void getOrders_success() {
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
                orderQueryService.getOrders(pageable);

        // then
        assertThat(response.getTotalElements()).isEqualTo(1);
        assertThat(response.getTotalPages()).isEqualTo(1);
        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().getFirst().orderNumber())
                .isEqualTo("ORD-20260805-123456");

        verify(orderRepository)
                .findAllByDeletedAtIsNull(pageable);
    }

    @Test
    @DisplayName("조회할 주문이 없으면 빈 페이지를 반환한다")
    void getOrders_empty() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        Page<Order> emptyPage = Page.empty(pageable);

        given(orderRepository.findAllByDeletedAtIsNull(pageable))
                .willReturn(emptyPage);

        // when
        Page<OrderSummaryResponse> response =
                orderQueryService.getOrders(pageable);

        // then
        assertThat(response.getContent()).isEmpty();
        assertThat(response.getTotalElements()).isZero();
        assertThat(response.getTotalPages()).isZero();

        verify(orderRepository)
                .findAllByDeletedAtIsNull(pageable);
    }
}