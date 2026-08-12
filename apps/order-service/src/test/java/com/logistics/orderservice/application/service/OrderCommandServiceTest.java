package com.logistics.orderservice.application.service;

import com.logistics.common.exception.BusinessException;
import com.logistics.common.security.principal.CustomUserDetails;
import com.logistics.orderservice.application.authorization.OrderAuthorization;
import com.logistics.orderservice.application.command.UpdateOrderCommand;
import com.logistics.orderservice.application.service.command.OrderManagementService;
import com.logistics.orderservice.domain.model.Order;
import com.logistics.orderservice.domain.repository.OrderRepository;
import com.logistics.orderservice.error.OrderErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderCommandServiceTest {

    @Mock OrderRepository orderRepository;
    @Mock OrderAuthorization orderAuthorization;
    @Mock Order order;

    private OrderManagementService service;
    private CustomUserDetails user;
    private UUID orderId;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        orderId = UUID.randomUUID();
        now = LocalDateTime.of(2026, 8, 12, 15, 0);
        Clock clock = Clock.fixed(now.atZone(ZoneId.of("Asia/Seoul")).toInstant(), ZoneId.of("Asia/Seoul"));
        service = new OrderManagementService(orderRepository, orderAuthorization, clock);
        user = CustomUserDetails.from(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "MASTER");
    }

    @Test
    @DisplayName("주문 수정 시 권한을 검증하고 고정된 현재 시각으로 도메인을 수정한다")
    void updateOrder_success() {
        UpdateOrderCommand command = new UpdateOrderCommand("변경 요청", now.plusDays(2));
        given(orderRepository.findByIdAndDeletedAtIsNull(orderId)).willReturn(Optional.of(order));

        service.updateOrder(user, command, orderId);

        verify(orderAuthorization).validateOrderManagementPermission(user, order);
        verify(order).update("변경 요청", now.plusDays(2), now);
    }

    @Test
    @DisplayName("없는 주문 수정 시 ORDER_NOT_FOUND 예외가 발생한다")
    void updateOrder_notFound() {
        given(orderRepository.findByIdAndDeletedAtIsNull(orderId)).willReturn(Optional.empty());
        UpdateOrderCommand command = new UpdateOrderCommand(null, null);

        assertNotFound(() -> service.updateOrder(user, command, orderId));
    }

    @Test
    @DisplayName("주문 삭제 시 권한을 검증하고 삭제 사용자를 기록한다")
    void deleteOrder_success() {
        given(orderRepository.findByIdAndDeletedAtIsNull(orderId)).willReturn(Optional.of(order));

        service.deleteOrder(user, orderId);

        verify(orderAuthorization).validateOrderManagementPermission(user, order);
        verify(order).delete(user.getId());
    }

    @Test
    @DisplayName("없는 주문 삭제 시 ORDER_NOT_FOUND 예외가 발생한다")
    void deleteOrder_notFound() {
        given(orderRepository.findByIdAndDeletedAtIsNull(orderId)).willReturn(Optional.empty());
        assertNotFound(() -> service.deleteOrder(user, orderId));
    }

    private void assertNotFound(Runnable action) {
        assertThatThrownBy(action::run)
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(OrderErrorCode.ORDER_NOT_FOUND));
    }
}
