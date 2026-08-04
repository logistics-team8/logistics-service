package com.logistics.orderservice.application.service;

import com.logistics.common.exception.BusinessException;
import com.logistics.orderservice.application.command.CreateOrderCommand;
import com.logistics.orderservice.application.command.CreateOrderItemCommand;
import com.logistics.orderservice.domain.model.Order;
import com.logistics.orderservice.domain.model.OrderItem;
import com.logistics.orderservice.domain.model.OrderItemStatus;
import com.logistics.orderservice.domain.model.OrderStatus;
import com.logistics.orderservice.domain.repository.OrderRepository;
import com.logistics.orderservice.presentation.dto.response.CreateOrderResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderCommandServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderCommandService orderCommandService;

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
                        new CreateOrderItemCommand(firstProductId, 2),
                        new CreateOrderItemCommand(secondProductId, 5)
                )
        );

        /*
         * 실제 DB 저장 대신 전달받은 Order를 그대로 반환한다.
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
                .matches("ORD-\\d{8}-\\d{6}");

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
        ).isInstanceOf(BusinessException.class);

        /*
         * Entity 검증에서 예외가 발생했으므로
         * Repository 저장은 호출되면 안 된다.
         */
        verify(orderRepository, never())
                .save(any(Order.class));
    }

    @Test
    @DisplayName("상품 ID가 null이면 예외가 발생한다")
    void createOrder_nullProductId_fail() {
        // given
        CreateOrderCommand command = new CreateOrderCommand(
                UUID.randomUUID(),
                UUID.randomUUID(),
                null,
                LocalDateTime.now().plusDays(1),
                List.of(
                        new CreateOrderItemCommand(null, 1)
                )
        );

        // when & then
        assertThatThrownBy(
                () -> orderCommandService.createOrder(command)
        ).isInstanceOf(BusinessException.class);

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
        ).isInstanceOf(BusinessException.class);

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
        ).isInstanceOf(BusinessException.class);

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
        ).isInstanceOf(BusinessException.class);

        verify(orderRepository, never())
                .save(any(Order.class));
    }
}