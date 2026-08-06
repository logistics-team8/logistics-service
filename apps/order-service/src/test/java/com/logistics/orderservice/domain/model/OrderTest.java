package com.logistics.orderservice.domain.model;

import com.logistics.common.exception.BusinessException;
import com.logistics.orderservice.error.OrderErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderTest {

    private UUID requesterId;
    private UUID receiverCompanyId;
    private LocalDateTime requestedDeliveryAt;

    @BeforeEach
    void setUp() {
        requesterId = UUID.randomUUID();
        receiverCompanyId = UUID.randomUUID();
        requestedDeliveryAt =
                LocalDateTime.of(2026, 8, 15, 14, 0);
    }

    private Order createOrder() {
        return Order.create(
                "ORD-20260806-ABCDEF123456",
                requesterId,
                receiverCompanyId,
                "기존 요청사항",
                requestedDeliveryAt
        );
    }

    @Nested
    @DisplayName("주문 생성")
    class CreateOrderTest {

        @Test
        @DisplayName("주문을 생성하면 기본 상태는 PENDING이다")
        void createOrder_success() {
            // when
            Order order = createOrder();

            // then
            assertThat(order.getOrderNumber())
                    .isEqualTo("ORD-20260806-ABCDEF123456");

            assertThat(order.getRequesterId())
                    .isEqualTo(requesterId);

            assertThat(order.getReceiverCompanyId())
                    .isEqualTo(receiverCompanyId);

            assertThat(order.getRequestMessage())
                    .isEqualTo("기존 요청사항");

            assertThat(order.getRequestedDeliveryAt())
                    .isEqualTo(requestedDeliveryAt);

            assertThat(order.getStatus())
                    .isEqualTo(OrderStatus.PENDING);

            assertThat(order.getOrderItems())
                    .isEmpty();

            assertThat(order.getCanceledBy())
                    .isNull();

            assertThat(order.getCanceledAt())
                    .isNull();
        }
    }

    @Nested
    @DisplayName("주문상품 추가")
    class AddOrderItemTest {

        @Test
        @DisplayName("주문에 상품을 추가한다")
        void addOrderItem_success() {
            // given
            Order order = createOrder();
            UUID productId = UUID.randomUUID();

            // when
            order.addOrderItem(productId, 3);

            // then
            assertThat(order.getOrderItems())
                    .hasSize(1);

            OrderItem orderItem =
                    order.getOrderItems().getFirst();

            assertThat(orderItem.getProductId())
                    .isEqualTo(productId);

            assertThat(orderItem.getQuantity())
                    .isEqualTo(3);

            assertThat(orderItem.getStatus())
                    .isEqualTo(OrderItemStatus.ACTIVE);

            assertThat(orderItem.getOrder())
                    .isSameAs(order);
        }

        @Test
        @DisplayName("하나의 주문에 동일한 상품을 중복으로 추가할 수 없다")
        void addOrderItem_duplicateProduct_fail() {
            // given
            Order order = createOrder();
            UUID productId = UUID.randomUUID();

            order.addOrderItem(productId, 2);

            // when & then
            assertThatThrownBy(() ->
                    order.addOrderItem(productId, 3)
            )
                    .isInstanceOf(BusinessException.class)
                    .satisfies(exception -> {
                        BusinessException businessException =
                                (BusinessException) exception;

                        assertThat(businessException.getErrorCode())
                                .isEqualTo(
                                        OrderErrorCode
                                                .DUPLICATE_ORDER_PRODUCT
                                );
                    });

            assertThat(order.getOrderItems())
                    .hasSize(1);
        }

        @Test
        @DisplayName("주문 수량이 null이면 상품을 추가할 수 없다")
        void addOrderItem_nullQuantity_fail() {
            // given
            Order order = createOrder();

            // when & then
            assertThatThrownBy(() ->
                    order.addOrderItem(
                            UUID.randomUUID(),
                            null
                    )
            )
                    .isInstanceOf(BusinessException.class)
                    .satisfies(exception -> {
                        BusinessException businessException =
                                (BusinessException) exception;

                        assertThat(businessException.getErrorCode())
                                .isEqualTo(
                                        OrderErrorCode
                                                .INVALID_ORDER_QUANTITY
                                );
                    });

            assertThat(order.getOrderItems())
                    .isEmpty();
        }

        @Test
        @DisplayName("주문 수량이 0이면 상품을 추가할 수 없다")
        void addOrderItem_zeroQuantity_fail() {
            // given
            Order order = createOrder();

            // when & then
            assertThatThrownBy(() ->
                    order.addOrderItem(
                            UUID.randomUUID(),
                            0
                    )
            )
                    .isInstanceOf(BusinessException.class)
                    .satisfies(exception -> {
                        BusinessException businessException =
                                (BusinessException) exception;

                        assertThat(businessException.getErrorCode())
                                .isEqualTo(
                                        OrderErrorCode
                                                .INVALID_ORDER_QUANTITY
                                );
                    });

            assertThat(order.getOrderItems())
                    .isEmpty();
        }

        @Test
        @DisplayName("주문 수량이 음수이면 상품을 추가할 수 없다")
        void addOrderItem_negativeQuantity_fail() {
            // given
            Order order = createOrder();

            // when & then
            assertThatThrownBy(() ->
                    order.addOrderItem(
                            UUID.randomUUID(),
                            -1
                    )
            )
                    .isInstanceOf(BusinessException.class)
                    .satisfies(exception -> {
                        BusinessException businessException =
                                (BusinessException) exception;

                        assertThat(businessException.getErrorCode())
                                .isEqualTo(
                                        OrderErrorCode
                                                .INVALID_ORDER_QUANTITY
                                );
                    });

            assertThat(order.getOrderItems())
                    .isEmpty();
        }
    }

    @Nested
    @DisplayName("주문 수정")
    class UpdateOrderTest {

        @Test
        @DisplayName("PENDING 주문의 요청사항과 희망 납품 일시를 수정한다")
        void updateOrder_success() {
            // given
            Order order = createOrder();

            String newRequestMessage =
                    "오후 5시 이전에 배송해주세요.";

            LocalDateTime newRequestedDeliveryAt =
                    LocalDateTime.of(
                            2026,
                            8,
                            20,
                            17,
                            0
                    );

            // when
            order.update(
                    newRequestMessage,
                    newRequestedDeliveryAt
            );

            // then
            assertThat(order.getRequestMessage())
                    .isEqualTo(newRequestMessage);

            assertThat(order.getRequestedDeliveryAt())
                    .isEqualTo(newRequestedDeliveryAt);

            assertThat(order.getStatus())
                    .isEqualTo(OrderStatus.PENDING);
        }

        @Test
        @DisplayName("CONFIRMED 주문은 수정할 수 없다")
        void updateOrder_confirmed_fail() {
            // given
            Order order = createOrder();

            changeStatus(
                    order,
                    OrderStatus.CONFIRMED
            );

            String originalMessage =
                    order.getRequestMessage();

            LocalDateTime originalDeliveryAt =
                    order.getRequestedDeliveryAt();

            // when & then
            assertThatThrownBy(() ->
                    order.update(
                            "변경 요청사항",
                            LocalDateTime.now().plusDays(10)
                    )
            )
                    .isInstanceOf(BusinessException.class)
                    .satisfies(exception -> {
                        BusinessException businessException =
                                (BusinessException) exception;

                        assertThat(businessException.getErrorCode())
                                .isEqualTo(
                                        OrderErrorCode
                                                .ORDER_NOT_UPDATABLE
                                );
                    });

            assertThat(order.getRequestMessage())
                    .isEqualTo(originalMessage);

            assertThat(order.getRequestedDeliveryAt())
                    .isEqualTo(originalDeliveryAt);
        }

        @Test
        @DisplayName("FAILED 주문은 수정할 수 없다")
        void updateOrder_failed_fail() {
            // given
            Order order = createOrder();

            changeStatus(
                    order,
                    OrderStatus.FAILED
            );

            // when & then
            assertThatThrownBy(() ->
                    order.update(
                            "변경 요청사항",
                            LocalDateTime.now().plusDays(10)
                    )
            )
                    .isInstanceOf(BusinessException.class)
                    .satisfies(exception -> {
                        BusinessException businessException =
                                (BusinessException) exception;

                        assertThat(businessException.getErrorCode())
                                .isEqualTo(
                                        OrderErrorCode
                                                .ORDER_NOT_UPDATABLE
                                );
                    });
        }

        @Test
        @DisplayName("CANCELED 주문은 수정할 수 없다")
        void updateOrder_canceled_fail() {
            // given
            Order order = createOrder();

            changeStatus(
                    order,
                    OrderStatus.CANCELED
            );

            // when & then
            assertThatThrownBy(() ->
                    order.update(
                            "변경 요청사항",
                            LocalDateTime.now().plusDays(10)
                    )
            )
                    .isInstanceOf(BusinessException.class)
                    .satisfies(exception -> {
                        BusinessException businessException =
                                (BusinessException) exception;

                        assertThat(businessException.getErrorCode())
                                .isEqualTo(
                                        OrderErrorCode
                                                .ORDER_NOT_UPDATABLE
                                );
                    });
        }
    }

    @Nested
    @DisplayName("주문 논리 삭제")
    class DeleteOrderTest {

        @Test
        @DisplayName("FAILED 주문을 논리 삭제한다")
        void deleteOrder_failed_success() {
            // given
            Order order = createOrder();
            UUID deletedBy = UUID.randomUUID();

            order.addOrderItem(
                    UUID.randomUUID(),
                    2
            );

            order.addOrderItem(
                    UUID.randomUUID(),
                    3
            );

            changeStatus(
                    order,
                    OrderStatus.FAILED
            );

            // when
            order.delete(deletedBy);

            // then
            assertThat(order.getDeletedAt())
                    .isNotNull();

            assertThat(order.getDeletedBy())
                    .isEqualTo(deletedBy);

            assertThat(order.getStatus())
                    .isEqualTo(OrderStatus.FAILED);

            assertThat(order.getOrderItems())
                    .allSatisfy(orderItem -> {
                        assertThat(orderItem.getDeletedAt())
                                .isNotNull();

                        assertThat(orderItem.getDeletedBy())
                                .isEqualTo(deletedBy);
                    });
        }

        @Test
        @DisplayName("CANCELED 주문을 논리 삭제한다")
        void deleteOrder_canceled_success() {
            // given
            Order order = createOrder();
            UUID deletedBy = UUID.randomUUID();

            order.addOrderItem(
                    UUID.randomUUID(),
                    2
            );

            changeStatus(
                    order,
                    OrderStatus.CANCELED
            );

            // when
            order.delete(deletedBy);

            // then
            assertThat(order.getDeletedAt())
                    .isNotNull();

            assertThat(order.getDeletedBy())
                    .isEqualTo(deletedBy);

            assertThat(order.getStatus())
                    .isEqualTo(OrderStatus.CANCELED);

            assertThat(order.getOrderItems())
                    .allSatisfy(orderItem -> {
                        assertThat(orderItem.getDeletedAt())
                                .isNotNull();

                        assertThat(orderItem.getDeletedBy())
                                .isEqualTo(deletedBy);
                    });
        }

        @Test
        @DisplayName("PENDING 주문은 삭제할 수 없다")
        void deleteOrder_pending_fail() {
            // given
            Order order = createOrder();
            UUID deletedBy = UUID.randomUUID();

            order.addOrderItem(
                    UUID.randomUUID(),
                    2
            );

            // when & then
            assertThatThrownBy(() ->
                    order.delete(deletedBy)
            )
                    .isInstanceOf(BusinessException.class)
                    .satisfies(exception -> {
                        BusinessException businessException =
                                (BusinessException) exception;

                        assertThat(businessException.getErrorCode())
                                .isEqualTo(
                                        OrderErrorCode
                                                .ORDER_NOT_DELETABLE
                                );
                    });

            assertThat(order.getDeletedAt())
                    .isNull();

            assertThat(order.getDeletedBy())
                    .isNull();

            assertThat(order.getOrderItems())
                    .allSatisfy(orderItem -> {
                        assertThat(orderItem.getDeletedAt())
                                .isNull();

                        assertThat(orderItem.getDeletedBy())
                                .isNull();
                    });
        }

        @Test
        @DisplayName("CONFIRMED 주문은 취소하지 않고 바로 삭제할 수 없다")
        void deleteOrder_confirmed_fail() {
            // given
            Order order = createOrder();
            UUID deletedBy = UUID.randomUUID();

            order.addOrderItem(
                    UUID.randomUUID(),
                    2
            );

            changeStatus(
                    order,
                    OrderStatus.CONFIRMED
            );

            // when & then
            assertThatThrownBy(() ->
                    order.delete(deletedBy)
            )
                    .isInstanceOf(BusinessException.class)
                    .satisfies(exception -> {
                        BusinessException businessException =
                                (BusinessException) exception;

                        assertThat(businessException.getErrorCode())
                                .isEqualTo(
                                        OrderErrorCode
                                                .ORDER_NOT_DELETABLE
                                );
                    });

            assertThat(order.getDeletedAt())
                    .isNull();

            assertThat(order.getDeletedBy())
                    .isNull();

            assertThat(order.getOrderItems())
                    .allSatisfy(orderItem -> {
                        assertThat(orderItem.getDeletedAt())
                                .isNull();

                        assertThat(orderItem.getDeletedBy())
                                .isNull();
                    });
        }

        @Test
        @DisplayName("논리 삭제해도 주문 상태와 주문상품 상태는 변경되지 않는다")
        void deleteOrder_doesNotChangeStatus() {
            // given
            Order order = createOrder();
            UUID deletedBy = UUID.randomUUID();

            order.addOrderItem(
                    UUID.randomUUID(),
                    2
            );

            changeStatus(
                    order,
                    OrderStatus.FAILED
            );

            OrderItem orderItem =
                    order.getOrderItems().getFirst();

            // when
            order.delete(deletedBy);

            // then
            assertThat(order.getStatus())
                    .isEqualTo(OrderStatus.FAILED);

            assertThat(orderItem.getStatus())
                    .isEqualTo(OrderItemStatus.ACTIVE);

            assertThat(order.getDeletedAt())
                    .isNotNull();

            assertThat(orderItem.getDeletedAt())
                    .isNotNull();
        }
    }

    /**
     * 현재 Order에 상태 전환 메서드가 공개되어 있지 않기 때문에
     * 도메인 상태별 테스트를 위해서만 ReflectionTestUtils를 사용한다.
     *
     * confirm(), fail(), cancel() 메서드가 구현되면
     * 해당 도메인 메서드 호출로 교체하는 것이 바람직하다.
     */
    private void changeStatus(
            Order order,
            OrderStatus status
    ) {
        ReflectionTestUtils.setField(
                order,
                "status",
                status
        );
    }
}