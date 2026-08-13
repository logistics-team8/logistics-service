package com.logistics.orderservice.application.service.command;

import com.logistics.common.exception.BusinessException;
import com.logistics.common.security.principal.CustomUserDetails;
import com.logistics.orderservice.application.command.CreateOrderCommand;
import com.logistics.orderservice.application.command.CreateOrderItemCommand;
import com.logistics.orderservice.application.exception.DeliveryStatusUnknownException;
import com.logistics.orderservice.application.port.CompanyPort;
import com.logistics.orderservice.application.port.DeliveryPort;
import com.logistics.orderservice.application.port.ProductPort;
import com.logistics.orderservice.application.port.UserPort;
import com.logistics.orderservice.domain.model.Order;
import com.logistics.orderservice.domain.model.OrderFailureReason;
import com.logistics.orderservice.domain.model.OrderStatus;
import com.logistics.orderservice.error.OrderErrorCode;
import com.logistics.orderservice.presentation.dto.response.CreateOrderResponse;
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
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class OrderCreateServiceTest {

    @Mock UserPort userPort;
    @Mock CompanyPort companyPort;
    @Mock ProductPort productPort;
    @Mock OrderStateService orderStateService;
    @Mock DeliveryRequestService deliveryRequestService;
    @Mock StockProcessService stockProcessService;

    private OrderCreateService service;
    private CustomUserDetails user;
    private LocalDateTime now;
    private UUID orderId;
    private UUID receiverCompanyId;
    private UUID receiverHubId;
    private UUID departureHubId;
    private UUID firstProductId;
    private UUID secondProductId;
    private CreateOrderCommand command;
    private List<ProductPort.ProductInfo> products;
    private UserPort.UserInfo receiver;
    private CompanyPort.CompanyInfo receiverCompany;
    private AtomicReference<Order> savedOrder;

    @BeforeEach
    void setUp() {
        now = LocalDateTime.of(2026, 8, 12, 10, 0);
        ZoneId zone = ZoneId.of("Asia/Seoul");
        Clock clock = Clock.fixed(now.atZone(zone).toInstant(), zone);
        service = new OrderCreateService(
                userPort, companyPort, productPort, clock, orderStateService,
                deliveryRequestService, stockProcessService
        );

        UUID userId = UUID.randomUUID();
        orderId = UUID.randomUUID();
        receiverCompanyId = UUID.randomUUID();
        receiverHubId = UUID.randomUUID();
        departureHubId = UUID.randomUUID();
        firstProductId = UUID.randomUUID();
        secondProductId = UUID.randomUUID();
        user = CustomUserDetails.from(userId, UUID.randomUUID(), UUID.randomUUID(), "COMPANY_MANAGER");

        command = new CreateOrderCommand(
                receiverCompanyId,
                "파손 주의",
                now.plusDays(3),
                List.of(
                        new CreateOrderItemCommand(firstProductId, 2),
                        new CreateOrderItemCommand(secondProductId, 5)
                )
        );
        products = List.of(
                new ProductPort.ProductInfo(firstProductId, "상품 A", UUID.randomUUID(), departureHubId),
                new ProductPort.ProductInfo(secondProductId, "상품 B", UUID.randomUUID(), departureHubId)
        );
        receiver = new UserPort.UserInfo(
                userId, "receiver", "홍길동", "hong.slack",
                receiverHubId, receiverCompanyId, "COMPANY_MANAGER"
        );
        receiverCompany = new CompanyPort.CompanyInfo(
                receiverCompanyId, receiverHubId, "수령 업체", "서울특별시 중구 세종대로 110"
        );
        savedOrder = new AtomicReference<>();
    }

    @Test
    @DisplayName("주문·재고·배송 처리에 성공하면 DELIVERY_CREATED 주문을 반환한다")
    void createOrder_success() {
        givenBaseDependencies();
        DeliveryPort.DeliveryInfo delivery = matchingDelivery();
        given(deliveryRequestService.requestDelivery(any())).willReturn(Optional.of(delivery));
        given(orderStateService.markDeliveryCreated(orderId)).willAnswer(invocation -> {
            Order order = savedOrder.get();
            order.confirm();
            order.markDeliveryCreated();
            return order;
        });

        CreateOrderResponse response = service.createOrder(
                command, user, "request-key", "request-hash"
        );

        Order order = savedOrder.get();
        assertThat(response.orderId()).isEqualTo(orderId);
        assertThat(response.status()).isEqualTo(OrderStatus.DELIVERY_CREATED);
        assertThat(order.getOrderNumber()).matches("ORD-20260812-[0-9A-F]{12}");
        assertThat(order.getReceiverCompanyId()).isEqualTo(receiverCompanyId);
        assertThat(order.getDestinationHubId()).isEqualTo(receiverHubId);
        assertThat(order.getDeliveryAddress()).isEqualTo(receiverCompany.address());
        assertThat(order.getReceiverName()).isEqualTo(receiver.name());
        assertThat(order.getReceiverSlackId()).isEqualTo(receiver.slackId());
        assertThat(order.getOrderItems()).extracting("productName")
                .containsExactly("상품 A", "상품 B");

        List<ProductPort.StockItem> expectedStock = List.of(
                new ProductPort.StockItem(firstProductId, 2),
                new ProductPort.StockItem(secondProductId, 5)
        );
        verify(stockProcessService).decreaseStock(orderId, expectedStock);
        verify(orderStateService).confirmOrder(orderId);

        ArgumentCaptor<DeliveryPort.CreateDeliveryCommand> deliveryCaptor =
                ArgumentCaptor.forClass(DeliveryPort.CreateDeliveryCommand.class);
        verify(deliveryRequestService).requestDelivery(deliveryCaptor.capture());
        assertThat(deliveryCaptor.getValue()).isEqualTo(new DeliveryPort.CreateDeliveryCommand(
                orderId, user.getId(), departureHubId, receiverHubId,
                receiverCompany.address(), receiver.name(), receiver.slackId()
        ));
        verify(orderStateService).markDeliveryCreated(orderId);
    }

    @Test
    @DisplayName("멱등 키와 요청 해시를 PENDING 주문에 저장한다")
    void createOrder_assignsIdempotencyMetadata() {
        givenBaseDependencies();
        given(deliveryRequestService.requestDelivery(any()))
                .willReturn(Optional.of(matchingDelivery()));
        given(orderStateService.markDeliveryCreated(orderId)).willAnswer(invocation -> {
            Order order = savedOrder.get();
            order.confirm();
            order.markDeliveryCreated();
            return order;
        });

        service.createOrder(command, user, "request-key", "request-hash");

        assertThat(savedOrder.get().getIdempotencyKey()).isEqualTo("request-key");
        assertThat(savedOrder.get().getRequestHash()).isEqualTo("request-hash");
    }

    @Test
    @DisplayName("중복 상품이 포함되면 외부 조회 전에 주문 생성을 거절한다")
    void createOrder_duplicateProducts() {
        CreateOrderCommand duplicated = new CreateOrderCommand(
                receiverCompanyId, null, now.plusDays(2),
                List.of(
                        new CreateOrderItemCommand(firstProductId, 1),
                        new CreateOrderItemCommand(firstProductId, 2)
                )
        );

        assertBusinessException(() -> service.createOrder(
                        duplicated, user, "request-key", "request-hash"),
                OrderErrorCode.DUPLICATE_ORDER_PRODUCT);
        verifyNoInteractions(productPort, userPort, companyPort, orderStateService,
                deliveryRequestService, stockProcessService);
    }

    @Test
    @DisplayName("요청 상품 중 조회되지 않은 상품이 있으면 주문을 저장하지 않는다")
    void createOrder_missingProduct() {
        given(productPort.getProducts(List.of(firstProductId, secondProductId)))
                .willReturn(List.of(products.getFirst()));

        assertBusinessException(() -> service.createOrder(
                        command, user, "request-key", "request-hash"),
                OrderErrorCode.PRODUCT_NOT_FOUND);
        verify(orderStateService, never()).createPendingOrder(any());
        verifyNoInteractions(userPort, companyPort, stockProcessService, deliveryRequestService);
    }

    @Test
    @DisplayName("상품들의 출발 허브가 다르면 주문을 저장하지 않는다")
    void createOrder_differentDepartureHubs() {
        List<ProductPort.ProductInfo> differentHubs = List.of(
                products.getFirst(),
                new ProductPort.ProductInfo(secondProductId, "상품 B", UUID.randomUUID(), UUID.randomUUID())
        );
        given(productPort.getProducts(List.of(firstProductId, secondProductId))).willReturn(differentHubs);

        assertBusinessException(() -> service.createOrder(
                        command, user, "request-key", "request-hash"),
                OrderErrorCode.DIFFERENT_DEPARTURE_HUB);
        verify(orderStateService, never()).createPendingOrder(any());
        verifyNoInteractions(userPort, companyPort, stockProcessService, deliveryRequestService);
    }

    @Test
    @DisplayName("배송 생성 결과를 알 수 없으면 재고를 복원하지 않고 확인 실패를 기록한다")
    void createOrder_deliveryStatusUnknown() {
        givenBaseDependencies();
        willThrow(new DeliveryStatusUnknownException("unknown"))
                .given(deliveryRequestService).requestDelivery(any());

        assertBusinessException(() -> service.createOrder(
                        command, user, "request-key", "request-hash"),
                OrderErrorCode.DELIVERY_STATUS_CHECK_FAILED);
        verify(orderStateService).markDeliveryStatusCheckFailed(orderId);
        verify(stockProcessService, never()).restoreStockAfterDeliveryFailure(any(), any());
        verify(orderStateService, never()).failOrder(any(), any());
    }

    @Test
    @DisplayName("재시도 후에도 배송이 없으면 재고를 복원하고 주문을 실패 처리한다")
    void createOrder_deliveryNotCreated() {
        givenBaseDependencies();
        given(deliveryRequestService.requestDelivery(any())).willReturn(Optional.empty());

        assertBusinessException(() -> service.createOrder(
                        command, user, "request-key", "request-hash"),
                OrderErrorCode.DELIVERY_CREATE_FAILED);

        List<ProductPort.StockItem> expectedStock = List.of(
                new ProductPort.StockItem(firstProductId, 2),
                new ProductPort.StockItem(secondProductId, 5)
        );
        verify(stockProcessService).restoreStockAfterDeliveryFailure(orderId, expectedStock);
        verify(orderStateService).failOrder(orderId, OrderFailureReason.DELIVERY_CREATE_FAILED);
        verify(orderStateService, never()).markDeliveryCreated(orderId);
    }

    private void givenBaseDependencies() {
        given(productPort.getProducts(List.of(firstProductId, secondProductId))).willReturn(products);
        given(userPort.getUserInfo(user.getId())).willReturn(receiver);
        given(companyPort.getCompanyInfo(receiverCompanyId)).willReturn(receiverCompany);
        given(orderStateService.createPendingOrder(any(Order.class))).willAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            ReflectionTestUtils.setField(order, "id", orderId);
            savedOrder.set(order);
            return order;
        });
    }

    private DeliveryPort.DeliveryInfo matchingDelivery() {
        return new DeliveryPort.DeliveryInfo(
                UUID.randomUUID(), orderId, user.getId(), "PENDING",
                departureHubId, receiverHubId, receiverCompany.address(),
                receiver.name(), receiver.slackId()
        );
    }

    private void assertBusinessException(Runnable action, OrderErrorCode expected) {
        assertThatThrownBy(action::run)
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(expected));
    }
}
