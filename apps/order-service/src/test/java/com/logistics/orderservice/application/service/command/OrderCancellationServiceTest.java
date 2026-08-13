package com.logistics.orderservice.application.service.command;

import com.logistics.common.exception.BusinessException;
import com.logistics.common.security.principal.CustomUserDetails;
import com.logistics.orderservice.application.authorization.OrderAuthorization;
import com.logistics.orderservice.application.port.ProductPort;
import com.logistics.orderservice.domain.model.Order;
import com.logistics.orderservice.domain.model.OrderStatus;
import com.logistics.orderservice.domain.repository.OrderRepository;
import com.logistics.orderservice.error.OrderErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderCancellationServiceTest {

    @Mock OrderRepository orderRepository;
    @Mock OrderAuthorization orderAuthorization;
    @Mock StockProcessService stockProcessService;
    @Mock OrderStateService orderStateService;

    private OrderCancelService service;
    private UUID orderId;
    private UUID userId;
    private LocalDateTime now;
    private CustomUserDetails user;

    @BeforeEach
    void setUp() {
        orderId = UUID.randomUUID();
        userId = UUID.randomUUID();
        now = LocalDateTime.of(2026, 8, 12, 12, 0);
        Clock clock = Clock.fixed(now.atZone(ZoneId.of("Asia/Seoul")).toInstant(), ZoneId.of("Asia/Seoul"));
        service = new OrderCancelService(orderRepository, orderAuthorization, stockProcessService, orderStateService, clock);
        user = CustomUserDetails.from(userId, UUID.randomUUID(), UUID.randomUUID(), "COMPANY_MANAGER");
    }

    @Test
    @DisplayName("CONFIRMED 주문 취소 시 활성 상품 재고를 복원하고 주문을 취소한다")
    void cancelOrder_confirmed() {
        Order order = order(OrderStatus.CONFIRMED);
        UUID productId = UUID.randomUUID();
        order.addOrderItem(productId, "상품", UUID.randomUUID(), UUID.randomUUID(), 3);
        Order canceled = order(OrderStatus.CANCELED);
        given(orderRepository.findByIdAndDeletedAtIsNull(orderId)).willReturn(Optional.of(order));
        given(orderStateService.cancelOrder(orderId, userId, now)).willReturn(canceled);

        service.cancelOrder(user, orderId);

        ArgumentCaptor<List<ProductPort.StockItem>> captor = ArgumentCaptor.forClass(List.class);
        verify(stockProcessService).restoreStockForCancel(org.mockito.ArgumentMatchers.eq(orderId), captor.capture());
        assertThat(captor.getValue()).containsExactly(new ProductPort.StockItem(productId, 3));
        verify(orderStateService).cancelOrder(orderId, userId, now);
    }

    @Test
    @DisplayName("PENDING 주문 취소 시 재고 복원을 요청하지 않는다")
    void cancelOrder_pending() {
        Order order = order(OrderStatus.PENDING);
        Order canceled = order(OrderStatus.CANCELED);
        given(orderRepository.findByIdAndDeletedAtIsNull(orderId)).willReturn(Optional.of(order));
        given(orderStateService.cancelOrder(orderId, userId, now)).willReturn(canceled);

        service.cancelOrder(user, orderId);

        verify(stockProcessService, never()).restoreStockForCancel(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(orderStateService).cancelOrder(orderId, userId, now);
    }

    @Test
    @DisplayName("존재하지 않는 주문은 취소할 수 없다")
    void cancelOrder_notFound() {
        given(orderRepository.findByIdAndDeletedAtIsNull(orderId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.cancelOrder(user, orderId))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(OrderErrorCode.ORDER_NOT_FOUND));
        verify(orderStateService, never()).cancelOrder(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    private Order order(OrderStatus status) {
        Order order = Order.create(
                "ORD-20260812-ABCDEF123456", userId, UUID.randomUUID(), UUID.randomUUID(),
                "서울", "홍길동", "hong.slack", null, now.plusDays(3), now
        );
        ReflectionTestUtils.setField(order, "id", orderId);
        ReflectionTestUtils.setField(order, "status", status);
        return order;
    }
}
