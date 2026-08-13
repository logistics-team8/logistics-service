package com.logistics.orderservice.application.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.logistics.common.exception.BusinessException;
import com.logistics.common.security.principal.CustomUserDetails;
import com.logistics.orderservice.application.command.CreateOrderCommand;
import com.logistics.orderservice.application.command.CreateOrderItemCommand;
import com.logistics.orderservice.domain.model.Order;
import com.logistics.orderservice.domain.repository.OrderRepository;
import com.logistics.orderservice.error.OrderErrorCode;
import com.logistics.orderservice.presentation.dto.response.CreateOrderResponse;
import java.time.LocalDateTime;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class IdempotentOrderCreateServiceTest {

    private static final String KEY = "order-request-001";

    @Mock OrderRepository orderRepository;
    @Mock OrderCreateService orderCreateService;

    private IdempotentOrderCreateService service;
    private CustomUserDetails user;
    private CreateOrderCommand command;

    @BeforeEach
    void setUp() {
        service = new IdempotentOrderCreateService(orderRepository, orderCreateService);
        user = CustomUserDetails.from(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "MASTER"
        );
        command = command(List.of(
                new CreateOrderItemCommand(UUID.fromString("00000000-0000-0000-0000-000000000001"), 1),
                new CreateOrderItemCommand(UUID.fromString("00000000-0000-0000-0000-000000000002"), 2)
        ));
    }

    @Test
    @DisplayName("처음 사용한 키면 정규화된 키와 요청 해시로 새 주문을 생성한다")
    void createOrder_newKey() {
        given(orderRepository.findByRequesterIdAndIdempotencyKey(user.getId(), KEY))
                .willReturn(Optional.empty());
        CreateOrderResponse created = response("주문이 생성되었습니다.");
        given(orderCreateService.createOrder(
                org.mockito.ArgumentMatchers.eq(command),
                org.mockito.ArgumentMatchers.eq(user),
                org.mockito.ArgumentMatchers.eq(KEY),
                anyString()
        )).willReturn(created);

        CreateOrderResponse result = service.createOrder("  " + KEY + "  ", command, user);

        assertThat(result).isSameAs(created);
        ArgumentCaptor<String> hashCaptor = ArgumentCaptor.forClass(String.class);
        verify(orderCreateService).createOrder(
                org.mockito.ArgumentMatchers.eq(command),
                org.mockito.ArgumentMatchers.eq(user),
                org.mockito.ArgumentMatchers.eq(KEY),
                hashCaptor.capture()
        );
        assertThat(hashCaptor.getValue()).matches("[0-9a-f]{64}");
    }

    @Test
    @DisplayName("같은 키와 같은 요청이면 외부 처리 없이 기존 주문을 반환한다")
    void createOrder_sameRequestReplayed() {
        String requestHash = captureHash(command);
        Order existing = existingOrder(requestHash);
        given(orderRepository.findByRequesterIdAndIdempotencyKey(user.getId(), KEY))
                .willReturn(Optional.of(existing));

        CreateOrderResponse result = service.createOrder(KEY, command, user);

        assertThat(result.orderId()).isEqualTo(existing.getId());
        assertThat(result.message()).isEqualTo("기존 주문이 존재합니다.");
        verify(orderCreateService, never()).createOrder(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    @DisplayName("상품 순서만 다르면 같은 주문 요청으로 판단한다")
    void createOrder_differentItemOrderReplayed() {
        String requestHash = captureHash(command);
        Order existing = existingOrder(requestHash);
        given(orderRepository.findByRequesterIdAndIdempotencyKey(user.getId(), KEY))
                .willReturn(Optional.of(existing));
        CreateOrderCommand reversed = command(List.of(
                command.items().get(1),
                command.items().get(0)
        ));

        CreateOrderResponse result = service.createOrder(KEY, reversed, user);

        assertThat(result.message()).isEqualTo("기존 주문이 존재합니다.");
        verify(orderCreateService, never()).createOrder(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    @DisplayName("같은 키에 주문 내용이 다르면 충돌 오류를 반환한다")
    void createOrder_sameKeyDifferentRequestRejected() {
        Order existing = existingOrder(captureHash(command));
        given(orderRepository.findByRequesterIdAndIdempotencyKey(user.getId(), KEY))
                .willReturn(Optional.of(existing));
        CreateOrderCommand changed = command(List.of(
                new CreateOrderItemCommand(command.items().getFirst().productId(), 99),
                command.items().get(1)
        ));

        assertBusinessException(
                () -> service.createOrder(KEY, changed, user),
                OrderErrorCode.IDEMPOTENCY_REQUEST_CONFLICT
        );
        verify(orderCreateService, never()).createOrder(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    @DisplayName("동시 요청으로 유니크 제약이 발생하면 먼저 저장된 주문을 반환한다")
    void createOrder_concurrentRequestReplayed() {
        String requestHash = captureHash(command);
        Order existing = existingOrder(requestHash);
        given(orderRepository.findByRequesterIdAndIdempotencyKey(user.getId(), KEY))
                .willReturn(Optional.empty(), Optional.of(existing));
        given(orderCreateService.createOrder(
                org.mockito.ArgumentMatchers.eq(command),
                org.mockito.ArgumentMatchers.eq(user),
                org.mockito.ArgumentMatchers.eq(KEY),
                anyString()
        )).willThrow(idempotencyConstraintViolation());

        CreateOrderResponse result = service.createOrder(KEY, command, user);

        assertThat(result.orderId()).isEqualTo(existing.getId());
        assertThat(result.message()).isEqualTo("기존 주문이 존재합니다.");
        verify(orderRepository, org.mockito.Mockito.times(2))
                .findByRequesterIdAndIdempotencyKey(user.getId(), KEY);
    }

    @Test
    @DisplayName("멱등성 제약 이외의 DB 무결성 오류는 기존 주문 응답으로 변환하지 않는다")
    void createOrder_otherIntegrityViolationRethrown() {
        DataIntegrityViolationException exception =
                new DataIntegrityViolationException(
                        "order status check constraint violation",
                        new ConstraintViolationException(
                                "check constraint violation",
                                new SQLException("check constraint violation"),
                                "p_orders_status_check"
                        )
                );
        given(orderRepository.findByRequesterIdAndIdempotencyKey(user.getId(), KEY))
                .willReturn(Optional.empty());
        given(orderCreateService.createOrder(
                org.mockito.ArgumentMatchers.eq(command),
                org.mockito.ArgumentMatchers.eq(user),
                org.mockito.ArgumentMatchers.eq(KEY),
                anyString()
        )).willThrow(exception);

        assertThatThrownBy(() -> service.createOrder(KEY, command, user))
                .isSameAs(exception);

        verify(orderRepository).findByRequesterIdAndIdempotencyKey(user.getId(), KEY);
    }

    @Test
    @DisplayName("멱등 키가 없거나 공백이거나 100자를 초과하면 거절한다")
    void createOrder_invalidKeyRejected() {
        assertBusinessException(
                () -> service.createOrder(null, command, user),
                OrderErrorCode.IDEMPOTENCY_KEY_INVALID
        );
        assertBusinessException(
                () -> service.createOrder("   ", command, user),
                OrderErrorCode.IDEMPOTENCY_KEY_INVALID
        );
        assertBusinessException(
                () -> service.createOrder("a".repeat(101), command, user),
                OrderErrorCode.IDEMPOTENCY_KEY_INVALID
        );
        verify(orderRepository, never())
                .findByRequesterIdAndIdempotencyKey(
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any()
                );
    }

    private String captureHash(CreateOrderCommand targetCommand) {
        given(orderRepository.findByRequesterIdAndIdempotencyKey(user.getId(), KEY))
                .willReturn(Optional.empty());
        given(orderCreateService.createOrder(
                org.mockito.ArgumentMatchers.eq(targetCommand),
                org.mockito.ArgumentMatchers.eq(user),
                org.mockito.ArgumentMatchers.eq(KEY),
                anyString()
        )).willReturn(response("주문이 생성되었습니다."));

        service.createOrder(KEY, targetCommand, user);

        ArgumentCaptor<String> hashCaptor = ArgumentCaptor.forClass(String.class);
        verify(orderCreateService).createOrder(
                org.mockito.ArgumentMatchers.eq(targetCommand),
                org.mockito.ArgumentMatchers.eq(user),
                org.mockito.ArgumentMatchers.eq(KEY),
                hashCaptor.capture()
        );
        org.mockito.Mockito.reset(orderRepository, orderCreateService);
        return hashCaptor.getValue();
    }

    private CreateOrderCommand command(List<CreateOrderItemCommand> items) {
        return new CreateOrderCommand(
                UUID.fromString("00000000-0000-0000-0000-000000000010"),
                "파손 주의",
                LocalDateTime.of(2026, 8, 16, 14, 0),
                items
        );
    }

    private DataIntegrityViolationException idempotencyConstraintViolation() {
        return new DataIntegrityViolationException(
                "duplicate idempotency key",
                new ConstraintViolationException(
                        "duplicate key",
                        new SQLException("duplicate key"),
                        "uk_order_requester_idempotency_key"
                )
        );
    }

    private Order existingOrder(String requestHash) {
        LocalDateTime now = LocalDateTime.of(2026, 8, 13, 10, 0);
        Order order = Order.create(
                "ORD-20260813-ABCDEF123456",
                user.getId(),
                command.receiverCompanyId(),
                UUID.randomUUID(),
                "서울시 중구",
                "홍길동",
                "hong.slack",
                command.requestMessage(),
                command.requestedDeliveryAt(),
                now
        );
        ReflectionTestUtils.setField(order, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(order, "createdAt", now);
        order.assignIdempotencyKey(KEY, requestHash);
        return order;
    }

    private CreateOrderResponse response(String message) {
        return new CreateOrderResponse(
                UUID.randomUUID(),
                "ORD-20260813-ABCDEF123456",
                com.logistics.orderservice.domain.model.OrderStatus.DELIVERY_CREATED,
                LocalDateTime.of(2026, 8, 13, 10, 0),
                message
        );
    }

    private void assertBusinessException(Runnable action, OrderErrorCode expected) {
        assertThatThrownBy(action::run)
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> assertThat(
                        ((BusinessException) exception).getErrorCode()
                ).isEqualTo(expected));
    }
}
