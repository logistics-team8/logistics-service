package com.logistics.orderservice.domain.model;

import com.logistics.common.exception.BusinessException;
import com.logistics.orderservice.error.OrderErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderTest {

    private LocalDateTime now;
    private UUID requesterId;
    private UUID companyId;
    private UUID destinationHubId;

    @BeforeEach
    void setUp() {
        now = LocalDateTime.of(2026, 8, 12, 10, 0);
        requesterId = UUID.randomUUID();
        companyId = UUID.randomUUID();
        destinationHubId = UUID.randomUUID();
    }

    @Test
    @DisplayName("주문 생성 시 배송 스냅샷과 PENDING 상태를 저장한다")
    void create_success() {
        Order order = createOrder();

        assertThat(order.getRequesterId()).isEqualTo(requesterId);
        assertThat(order.getReceiverCompanyId()).isEqualTo(companyId);
        assertThat(order.getDestinationHubId()).isEqualTo(destinationHubId);
        assertThat(order.getDeliveryAddress()).isEqualTo("서울특별시 중구 세종대로 110");
        assertThat(order.getReceiverName()).isEqualTo("홍길동");
        assertThat(order.getReceiverSlackId()).isEqualTo("hong.slack");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    @DisplayName("희망 납품일이 하루 미만이면 주문을 생성할 수 없다")
    void create_invalidDeliveryDate() {
        assertBusinessException(() -> createOrder(now.plusHours(23)),
                OrderErrorCode.INVALID_REQUESTED_DELIVERY_AT);
    }

    @Test
    @DisplayName("상품 스냅샷을 포함해 주문 상품을 추가한다")
    void addOrderItem_success() {
        Order order = createOrder();
        UUID productId = UUID.randomUUID();
        UUID supplierId = UUID.randomUUID();
        UUID departureHubId = UUID.randomUUID();

        order.addOrderItem(productId, "노트북", supplierId, departureHubId, 2);

        OrderItem item = order.getOrderItems().getFirst();
        assertThat(item.getProductId()).isEqualTo(productId);
        assertThat(item.getProductName()).isEqualTo("노트북");
        assertThat(item.getSupplierCompanyId()).isEqualTo(supplierId);
        assertThat(item.getDepartureHubId()).isEqualTo(departureHubId);
        assertThat(item.getQuantity()).isEqualTo(2);
    }

    @Test
    @DisplayName("동일한 상품은 한 주문에 중복 추가할 수 없다")
    void addOrderItem_duplicate() {
        Order order = createOrder();
        UUID productId = UUID.randomUUID();
        addItem(order, productId, 1);

        assertBusinessException(() -> addItem(order, productId, 2),
                OrderErrorCode.DUPLICATE_ORDER_PRODUCT);
    }

    @Test
    @DisplayName("재고 차감 성공 후 주문을 확정한다")
    void confirm_success() {
        Order order = createOrder();
        order.confirm();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    @DisplayName("재고 차감 결과 미확인 시 PENDING을 유지하고 실패 사유를 기록한다")
    void markStockDecreaseUnknown() {
        Order order = createOrder();
        order.markStockDecreaseUnknown();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(order.getFailureReason()).isEqualTo(OrderFailureReason.STOCK_DECREASE_UNKNOWN);
    }

    @Test
    @DisplayName("배송 생성이 확인되면 DELIVERY_CREATED 상태가 된다")
    void markDeliveryCreated_success() {
        Order order = createOrder();
        order.confirm();
        order.markDeliveryCreated();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.DELIVERY_CREATED);
    }

    @Test
    @DisplayName("주문 취소 시 활성 주문 상품도 모두 취소한다")
    void cancel_success() {
        Order order = createOrder();
        addItem(order, UUID.randomUUID(), 2);
        LocalDateTime canceledAt = now.plusHours(1);

        order.cancel(requesterId, canceledAt);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELED);
        assertThat(order.getCanceledBy()).isEqualTo(requesterId);
        assertThat(order.getCanceledAt()).isEqualTo(canceledAt);
        assertThat(order.getOrderItems()).allMatch(OrderItem::isCanceled);
    }

    @Test
    @DisplayName("DELIVERY_CREATED 주문은 취소 불가 예외가 발생한다")
    void cancel_deliveryCreatedOrder() {
        Order order = createOrder();
        order.confirm();
        order.markDeliveryCreated();

        assertBusinessException(() -> order.cancel(requesterId, now),
                OrderErrorCode.ORDER_NOT_CANCELABLE);
    }

    @Test
    @DisplayName("주문 상품 하나를 취소하고 모두 취소되면 주문도 취소한다")
    void cancelOrderItem_success() {
        Order order = createOrder();
        addItem(order, UUID.randomUUID(), 1);
        OrderItem item = order.getOrderItems().getFirst();
        UUID itemId = UUID.randomUUID();
        ReflectionTestUtils.setField(item, "id", itemId);

        order.cancelOrderItem(itemId, requesterId);

        assertThat(item.isCanceled()).isTrue();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELED);
    }

    private Order createOrder() {
        return createOrder(now.plusDays(3));
    }

    private Order createOrder(LocalDateTime deliveryAt) {
        return Order.create(
                "ORD-20260812-ABCDEF123456", requesterId, companyId,
                destinationHubId, "서울특별시 중구 세종대로 110", "홍길동",
                "hong.slack", "안전 배송", deliveryAt, now
        );
    }

    private void addItem(Order order, UUID productId, int quantity) {
        order.addOrderItem(productId, "상품", UUID.randomUUID(), UUID.randomUUID(), quantity);
    }

    private void assertBusinessException(Runnable action, OrderErrorCode errorCode) {
        assertThatThrownBy(action::run)
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(errorCode));
    }
}
