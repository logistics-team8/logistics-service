package com.logistics.orderservice.application.service;

import com.logistics.common.exception.BusinessException;
import com.logistics.orderservice.application.command.CreateOrderCommand;
import com.logistics.orderservice.application.command.CreateOrderItemCommand;
import com.logistics.orderservice.application.command.UpdateOrderCommand;
import com.logistics.orderservice.domain.model.Order;
import com.logistics.orderservice.domain.model.OrderItem;
import com.logistics.orderservice.domain.model.OrderItemStatus;
import com.logistics.orderservice.domain.model.OrderStatus;
import com.logistics.orderservice.domain.repository.OrderRepository;
import com.logistics.orderservice.error.OrderErrorCode;
import com.logistics.orderservice.presentation.dto.response.CreateOrderResponse;
import com.logistics.orderservice.presentation.dto.response.DeleteOrderResponse;
import com.logistics.orderservice.presentation.dto.response.UpdateOrderResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderCommandServiceTest {

    @Mock
    private OrderRepository orderRepository;

    /*
     * 수정·삭제 Service 테스트용 Mock Order.
     *
     * 생성 테스트에서는 실제 Order 객체를 사용하고,
     * 수정·삭제 테스트에서는 도메인 메서드 호출 여부를 확인하기 위해
     * Mock Order를 사용한다.
     */
    @Mock
    private Order mockOrder;

    @InjectMocks
    private OrderCommandService orderCommandService;

    private UUID orderId;
    private UUID userId;
    private String orderNumber;

    @BeforeEach
    void setUp() {
        orderId = UUID.randomUUID();
        userId = UUID.randomUUID();
        orderNumber = "ORD-20260806-ABCDEF123456";
    }

    // =========================================================
    // 주문 생성
    // =========================================================

    @Test
    @DisplayName("주문 생성에 성공한다")
    void createOrder_success() {
        // given
        UUID requesterId = UUID.randomUUID();
        UUID receiverCompanyId = UUID.randomUUID();
        UUID firstProductId = UUID.randomUUID();
        UUID secondProductId = UUID.randomUUID();

        LocalDateTime requestedDeliveryAt =
                LocalDateTime.now().plusDays(3);

        CreateOrderCommand command = new CreateOrderCommand(
                requesterId,
                receiverCompanyId,
                "오후 3시까지 배송해주세요.",
                requestedDeliveryAt,
                List.of(
                        new CreateOrderItemCommand(
                                firstProductId,
                                2
                        ),
                        new CreateOrderItemCommand(
                                secondProductId,
                                5
                        )
                )
        );

        /*
         * 실제 DB 저장 대신 전달받은 Order 객체를 그대로 반환한다.
         */
        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        CreateOrderResponse response =
                orderCommandService.createOrder(command);

        // then
        ArgumentCaptor<Order> orderCaptor =
                ArgumentCaptor.forClass(Order.class);

        verify(orderRepository, times(1))
                .save(orderCaptor.capture());

        Order savedOrder = orderCaptor.getValue();

        assertThat(savedOrder.getRequesterId())
                .isEqualTo(requesterId);

        assertThat(savedOrder.getReceiverCompanyId())
                .isEqualTo(receiverCompanyId);

        assertThat(savedOrder.getRequestMessage())
                .isEqualTo("오후 3시까지 배송해주세요.");

        assertThat(savedOrder.getRequestedDeliveryAt())
                .isEqualTo(requestedDeliveryAt);

        assertThat(savedOrder.getStatus())
                .isEqualTo(OrderStatus.PENDING);

        assertThat(savedOrder.getOrderNumber())
                .matches("ORD-\\d{8}-[0-9A-F]{12}");

        assertThat(savedOrder.getOrderItems())
                .hasSize(2);

        OrderItem firstOrderItem =
                savedOrder.getOrderItems().get(0);

        OrderItem secondOrderItem =
                savedOrder.getOrderItems().get(1);

        assertThat(firstOrderItem.getProductId())
                .isEqualTo(firstProductId);

        assertThat(firstOrderItem.getQuantity())
                .isEqualTo(2);

        assertThat(firstOrderItem.getStatus())
                .isEqualTo(OrderItemStatus.ACTIVE);

        assertThat(firstOrderItem.getOrder())
                .isSameAs(savedOrder);

        assertThat(secondOrderItem.getProductId())
                .isEqualTo(secondProductId);

        assertThat(secondOrderItem.getQuantity())
                .isEqualTo(5);

        assertThat(secondOrderItem.getStatus())
                .isEqualTo(OrderItemStatus.ACTIVE);

        assertThat(secondOrderItem.getOrder())
                .isSameAs(savedOrder);

        assertThat(response.orderNumber())
                .isEqualTo(savedOrder.getOrderNumber());

        assertThat(response.status())
                .isEqualTo(OrderStatus.PENDING);
    }

    @Test
    @DisplayName("하나의 주문에 동일한 상품이 중복되면 예외가 발생한다")
    void createOrder_duplicateProduct_fail() {
        // given
        UUID duplicatedProductId = UUID.randomUUID();

        CreateOrderCommand command = new CreateOrderCommand(
                UUID.randomUUID(),
                UUID.randomUUID(),
                null,
                LocalDateTime.now().plusDays(1),
                List.of(
                        new CreateOrderItemCommand(
                                duplicatedProductId,
                                2
                        ),
                        new CreateOrderItemCommand(
                                duplicatedProductId,
                                3
                        )
                )
        );

        // when & then
        assertThatThrownBy(
                () -> orderCommandService.createOrder(command)
        )
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> {
                    BusinessException businessException =
                            (BusinessException) exception;

                    assertThat(businessException.getErrorCode())
                            .isEqualTo(
                                    OrderErrorCode.DUPLICATE_ORDER_PRODUCT
                            );
                });

        verify(orderRepository, never())
                .save(any(Order.class));
    }

    @Test
    @DisplayName("주문 수량이 null이면 예외가 발생한다")
    void createOrder_nullQuantity_fail() {
        // given
        CreateOrderCommand command = new CreateOrderCommand(
                UUID.randomUUID(),
                UUID.randomUUID(),
                null,
                LocalDateTime.now().plusDays(1),
                List.of(
                        new CreateOrderItemCommand(
                                UUID.randomUUID(),
                                null
                        )
                )
        );

        // when & then
        assertThatThrownBy(
                () -> orderCommandService.createOrder(command)
        )
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> {
                    BusinessException businessException =
                            (BusinessException) exception;

                    assertThat(businessException.getErrorCode())
                            .isEqualTo(
                                    OrderErrorCode.INVALID_ORDER_QUANTITY
                            );
                });

        verify(orderRepository, never())
                .save(any(Order.class));
    }

    @Test
    @DisplayName("주문 수량이 0이면 예외가 발생한다")
    void createOrder_zeroQuantity_fail() {
        // given
        CreateOrderCommand command = new CreateOrderCommand(
                UUID.randomUUID(),
                UUID.randomUUID(),
                null,
                LocalDateTime.now().plusDays(1),
                List.of(
                        new CreateOrderItemCommand(
                                UUID.randomUUID(),
                                0
                        )
                )
        );

        // when & then
        assertThatThrownBy(
                () -> orderCommandService.createOrder(command)
        )
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> {
                    BusinessException businessException =
                            (BusinessException) exception;

                    assertThat(businessException.getErrorCode())
                            .isEqualTo(
                                    OrderErrorCode.INVALID_ORDER_QUANTITY
                            );
                });

        verify(orderRepository, never())
                .save(any(Order.class));
    }

    @Test
    @DisplayName("주문 수량이 음수이면 예외가 발생한다")
    void createOrder_negativeQuantity_fail() {
        // given
        CreateOrderCommand command = new CreateOrderCommand(
                UUID.randomUUID(),
                UUID.randomUUID(),
                null,
                LocalDateTime.now().plusDays(1),
                List.of(
                        new CreateOrderItemCommand(
                                UUID.randomUUID(),
                                -1
                        )
                )
        );

        // when & then
        assertThatThrownBy(
                () -> orderCommandService.createOrder(command)
        )
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> {
                    BusinessException businessException =
                            (BusinessException) exception;

                    assertThat(businessException.getErrorCode())
                            .isEqualTo(
                                    OrderErrorCode.INVALID_ORDER_QUANTITY
                            );
                });

        verify(orderRepository, never())
                .save(any(Order.class));
    }

    // =========================================================
    // 주문 수정
    // =========================================================

    @Test
    @DisplayName("주문의 요청사항과 희망 납품 일시를 수정한다")
    void updateOrder_success() {
        // given
        String requestMessage =
                "문 앞에 배송해주세요.";

        LocalDateTime requestedDeliveryAt =
                LocalDateTime.of(
                        2026,
                        8,
                        15,
                        14,
                        0
                );

        LocalDateTime updatedAt =
                LocalDateTime.of(
                        2026,
                        8,
                        6,
                        12,
                        0
                );

        UpdateOrderCommand command =
                new UpdateOrderCommand(
                        requestMessage,
                        requestedDeliveryAt
                );

        given(
                orderRepository.findByIdAndDeletedAtIsNull(
                        orderId
                )
        ).willReturn(Optional.of(mockOrder));

        /*
         * UpdateOrderResponse.from(order)에서 조회하는 값이다.
         *
         * Mockito 단위 테스트에서는 JPA Auditing이 동작하지 않으므로
         * updatedAt과 updatedBy를 직접 지정한다.
         */
        given(mockOrder.getId())
                .willReturn(orderId);

        given(mockOrder.getOrderNumber())
                .willReturn(orderNumber);

        given(mockOrder.getRequestMessage())
                .willReturn(requestMessage);

        given(mockOrder.getRequestedDeliveryAt())
                .willReturn(requestedDeliveryAt);

        given(mockOrder.getUpdatedAt())
                .willReturn(updatedAt);

        /*
         * 현재 AuditorAware 미적용 상태이므로 null을 반환한다.
         *
         * Mockito는 기본적으로 객체 반환값을 null로 반환하므로
         * 이 stubbing은 생략해도 된다.
         */
        given(mockOrder.getUpdatedBy())
                .willReturn(null);

        // when
        UpdateOrderResponse response =
                orderCommandService.updateOrder(
                        userId,
                        command,
                        orderId
                );

        // then
        assertThat(response).isNotNull();

        assertThat(response.orderId())
                .isEqualTo(orderId);

        assertThat(response.orderNumber())
                .isEqualTo(orderNumber);

        assertThat(response.requestMessage())
                .isEqualTo(requestMessage);

        assertThat(response.requestDeliverAt())
                .isEqualTo(requestedDeliveryAt);

        assertThat(response.updatedAt())
                .isEqualTo(updatedAt);

        assertThat(response.updateBy())
                .isNull();

        verify(orderRepository)
                .findByIdAndDeletedAtIsNull(orderId);

        verify(mockOrder)
                .update(
                        requestMessage,
                        requestedDeliveryAt
                );

        /*
         * 조회한 Order는 실제 환경에서 영속 상태이므로
         * 변경 감지로 수정된다.
         */
        verify(orderRepository, never())
                .save(any(Order.class));
    }

    @Test
    @DisplayName("수정할 주문이 존재하지 않으면 ORDER_NOT_FOUND 예외가 발생한다")
    void updateOrder_notFound() {
        // given
        UpdateOrderCommand command =
                new UpdateOrderCommand(
                        "수정 요청사항",
                        LocalDateTime.of(
                                2026,
                                8,
                                15,
                                14,
                                0
                        )
                );

        given(
                orderRepository.findByIdAndDeletedAtIsNull(
                        orderId
                )
        ).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() ->
                orderCommandService.updateOrder(
                        userId,
                        command,
                        orderId
                )
        )
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> {
                    BusinessException businessException =
                            (BusinessException) exception;

                    assertThat(
                            businessException.getErrorCode()
                    ).isEqualTo(
                            OrderErrorCode.ORDER_NOT_FOUND
                    );
                });

        verify(orderRepository)
                .findByIdAndDeletedAtIsNull(orderId);

        verify(mockOrder, never())
                .update(
                        command.requestMessage(),
                        command.requestDeliveryAt()
                );

        verify(orderRepository, never())
                .save(any(Order.class));
    }

    @Test
    @DisplayName("수정할 수 없는 주문 상태이면 ORDER_NOT_UPDATABLE 예외가 발생한다")
    void updateOrder_notUpdatable() {
        // given
        UpdateOrderCommand command =
                new UpdateOrderCommand(
                        "수정 요청사항",
                        LocalDateTime.of(
                                2026,
                                8,
                                15,
                                14,
                                0
                        )
                );

        given(
                orderRepository.findByIdAndDeletedAtIsNull(
                        orderId
                )
        ).willReturn(Optional.of(mockOrder));

        willThrow(
                new BusinessException(
                        OrderErrorCode.ORDER_NOT_UPDATABLE
                )
        ).given(mockOrder)
                .update(
                        command.requestMessage(),
                        command.requestDeliveryAt()
                );

        // when & then
        assertThatThrownBy(() ->
                orderCommandService.updateOrder(
                        userId,
                        command,
                        orderId
                )
        )
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> {
                    BusinessException businessException =
                            (BusinessException) exception;

                    assertThat(
                            businessException.getErrorCode()
                    ).isEqualTo(
                            OrderErrorCode.ORDER_NOT_UPDATABLE
                    );
                });

        verify(orderRepository)
                .findByIdAndDeletedAtIsNull(orderId);

        verify(mockOrder)
                .update(
                        command.requestMessage(),
                        command.requestDeliveryAt()
                );

        verify(orderRepository, never())
                .save(any(Order.class));
    }

    // =========================================================
    // 주문 삭제
    // =========================================================

    @Test
    @DisplayName("삭제 가능한 주문을 논리 삭제한다")
    void deleteOrder_success() {
        // given
        LocalDateTime deletedAt =
                LocalDateTime.of(
                        2026,
                        8,
                        6,
                        13,
                        0
                );

        given(
                orderRepository.findByIdAndDeletedAtIsNull(
                        orderId
                )
        ).willReturn(Optional.of(mockOrder));

        /*
         * DeleteOrderResponse.from(order)에서 사용하는 값이다.
         */
        given(mockOrder.getId())
                .willReturn(orderId);

        given(mockOrder.getDeletedAt())
                .willReturn(deletedAt);

        given(mockOrder.getDeletedBy())
                .willReturn(userId);

        // when
        DeleteOrderResponse response =
                orderCommandService.deleteOrder(
                        userId,
                        orderId
                );

        // then
        assertThat(response).isNotNull();

        assertThat(response.orderId())
                .isEqualTo(orderId);

        assertThat(response.deletedAt())
                .isEqualTo(deletedAt);

        assertThat(response.deletedBy())
                .isEqualTo(userId);

        verify(orderRepository)
                .findByIdAndDeletedAtIsNull(orderId);

        verify(mockOrder)
                .delete(userId);

        /*
         * 논리 삭제도 실제 환경에서는 영속 엔티티 변경 감지로
         * 반영되므로 save()를 다시 호출하지 않는다.
         */
        verify(orderRepository, never())
                .save(any(Order.class));
    }

    @Test
    @DisplayName("삭제할 주문이 존재하지 않으면 ORDER_NOT_FOUND 예외가 발생한다")
    void deleteOrder_notFound() {
        // given
        given(
                orderRepository.findByIdAndDeletedAtIsNull(
                        orderId
                )
        ).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() ->
                orderCommandService.deleteOrder(
                        userId,
                        orderId
                )
        )
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> {
                    BusinessException businessException =
                            (BusinessException) exception;

                    assertThat(
                            businessException.getErrorCode()
                    ).isEqualTo(
                            OrderErrorCode.ORDER_NOT_FOUND
                    );
                });

        verify(orderRepository)
                .findByIdAndDeletedAtIsNull(orderId);

        verify(mockOrder, never())
                .delete(userId);

        verify(orderRepository, never())
                .save(any(Order.class));
    }

    @Test
    @DisplayName("삭제할 수 없는 주문 상태이면 ORDER_NOT_DELETABLE 예외가 발생한다")
    void deleteOrder_notDeletable() {
        // given
        given(
                orderRepository.findByIdAndDeletedAtIsNull(
                        orderId
                )
        ).willReturn(Optional.of(mockOrder));

        willThrow(
                new BusinessException(
                        OrderErrorCode.ORDER_NOT_DELETABLE
                )
        ).given(mockOrder)
                .delete(userId);

        // when & then
        assertThatThrownBy(() ->
                orderCommandService.deleteOrder(
                        userId,
                        orderId
                )
        )
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> {
                    BusinessException businessException =
                            (BusinessException) exception;

                    assertThat(
                            businessException.getErrorCode()
                    ).isEqualTo(
                            OrderErrorCode.ORDER_NOT_DELETABLE
                    );
                });

        verify(orderRepository)
                .findByIdAndDeletedAtIsNull(orderId);

        verify(mockOrder)
                .delete(userId);

        verify(orderRepository, never())
                .save(any(Order.class));
    }
}