package com.logistics.orderservice.application.service;

import com.logistics.common.exception.BusinessException;
import com.logistics.common.security.principal.CustomUserDetails;
import com.logistics.orderservice.domain.model.Order;
import com.logistics.orderservice.domain.model.OrderItem;
import com.logistics.orderservice.domain.model.OrderItemStatus;
import com.logistics.orderservice.domain.model.OrderStatus;
import com.logistics.orderservice.domain.repository.OrderRepository;
import com.logistics.orderservice.error.OrderErrorCode;
import com.logistics.orderservice.presentation.dto.response.CancelOrderItemResponse;
import com.logistics.orderservice.presentation.dto.response.CancelOrderResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class OrderCancellationServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private Order order;
    @Mock private OrderItem orderItem;

    private OrderCommandService orderCommandService;
    private UUID orderId;
    private UUID orderItemId;
    private UUID userId;
    private CustomUserDetails requester;

    @BeforeEach
    void setUp() {
        orderId = UUID.randomUUID();
        orderItemId = UUID.randomUUID();
        userId = UUID.randomUUID();
        requester = CustomUserDetails.from(
                userId, UUID.randomUUID(), UUID.randomUUID(), "COMPANY_MANAGER"
        );
        Clock clock = Clock.fixed(
                Instant.parse("2026-08-09T03:00:00Z"), ZoneId.of("Asia/Seoul")
        );
        orderCommandService = new OrderCommandService(orderRepository, clock);
    }

    @Nested
    @DisplayName("주문 취소")
    class CancelOrderTest {

        @Test
        @DisplayName("주문자는 자신의 주문을 취소할 수 있다")
        void cancelOrder_requester_success() {
            LocalDateTime canceledAt = LocalDateTime.of(2026, 8, 9, 12, 0);
            given(orderRepository.findByIdAndDeletedAtIsNull(orderId)).willReturn(Optional.of(order));
            given(order.getRequesterId()).willReturn(userId);
            given(order.getId()).willReturn(orderId);
            given(order.getOrderNumber()).willReturn("ORD-20260809-ABCDEF123456");
            given(order.getStatus()).willReturn(OrderStatus.CANCELED);
            given(order.getCanceledAt()).willReturn(canceledAt);

            CancelOrderResponse response = orderCommandService.cancelOrder(requester, orderId);

            verify(order).cancel(userId);
            assertThat(response.orderId()).isEqualTo(orderId);
            assertThat(response.status()).isEqualTo(OrderStatus.CANCELED);
            assertThat(response.canceledAt()).isEqualTo(canceledAt);
        }

        @Test
        @DisplayName("MASTER는 다른 사용자의 주문도 취소할 수 있다")
        void cancelOrder_master_success() {
            CustomUserDetails master = CustomUserDetails.from(userId, null, null, "MASTER");
            given(orderRepository.findByIdAndDeletedAtIsNull(orderId)).willReturn(Optional.of(order));

            orderCommandService.cancelOrder(master, orderId);

            verify(order).cancel(userId);
        }

        @Test
        @DisplayName("취소 권한이 없으면 ORDER_ACCESS_DENIED 예외가 발생한다")
        void cancelOrder_accessDenied() {
            given(orderRepository.findByIdAndDeletedAtIsNull(orderId)).willReturn(Optional.of(order));
            given(order.getRequesterId()).willReturn(UUID.randomUUID());

            assertThatThrownBy(() -> orderCommandService.cancelOrder(requester, orderId))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(exception -> assertThat(((BusinessException) exception).getErrorCode())
                            .isEqualTo(OrderErrorCode.ORDER_ACCESS_DENIED));

            verify(order, never()).cancel(userId);
        }

        @Test
        @DisplayName("취소할 주문이 없으면 ORDER_NOT_FOUND 예외가 발생한다")
        void cancelOrder_notFound() {
            given(orderRepository.findByIdAndDeletedAtIsNull(orderId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> orderCommandService.cancelOrder(requester, orderId))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(exception -> assertThat(((BusinessException) exception).getErrorCode())
                            .isEqualTo(OrderErrorCode.ORDER_NOT_FOUND));

            verifyNoInteractions(order);
        }
    }

    @Nested
    @DisplayName("주문상품 취소")
    class CancelOrderItemTest {

        @Test
        @DisplayName("주문자는 자신의 주문상품을 취소할 수 있다")
        void cancelOrderItem_requester_success() {
            LocalDateTime canceledAt = LocalDateTime.of(2026, 8, 9, 12, 0);
            given(orderRepository.findByIdAndDeletedAtIsNull(orderId)).willReturn(Optional.of(order));
            given(order.getRequesterId()).willReturn(userId);
            given(order.cancelOrderItem(orderItemId, userId)).willReturn(orderItem);
            given(order.getId()).willReturn(orderId);
            given(order.getStatus()).willReturn(OrderStatus.PENDING);
            given(orderItem.getId()).willReturn(orderItemId);
            given(orderItem.getStatus()).willReturn(OrderItemStatus.CANCELED);
            given(orderItem.getCanceledAt()).willReturn(canceledAt);

            CancelOrderItemResponse response =
                    orderCommandService.cancelOrderItem(requester, orderId, orderItemId);

            verify(order).cancelOrderItem(orderItemId, userId);
            assertThat(response.orderId()).isEqualTo(orderId);
            assertThat(response.orderItemId()).isEqualTo(orderItemId);
            assertThat(response.orderStatus()).isEqualTo(OrderStatus.PENDING);
            assertThat(response.orderItemStatus()).isEqualTo(OrderItemStatus.CANCELED);
            assertThat(response.canceledAt()).isEqualTo(canceledAt);
        }

        @Test
        @DisplayName("권한이 없으면 주문상품 취소 도메인 메서드를 호출하지 않는다")
        void cancelOrderItem_accessDenied() {
            given(orderRepository.findByIdAndDeletedAtIsNull(orderId)).willReturn(Optional.of(order));
            given(order.getRequesterId()).willReturn(UUID.randomUUID());

            assertThatThrownBy(() -> orderCommandService.cancelOrderItem(requester, orderId, orderItemId))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(exception -> assertThat(((BusinessException) exception).getErrorCode())
                            .isEqualTo(OrderErrorCode.ORDER_ACCESS_DENIED));

            verify(order, never()).cancelOrderItem(orderItemId, userId);
        }
    }
}
