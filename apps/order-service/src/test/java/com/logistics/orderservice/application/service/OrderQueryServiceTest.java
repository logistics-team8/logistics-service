package com.logistics.orderservice.application.service;

import com.logistics.common.exception.BusinessException;
import com.logistics.common.security.principal.CustomUserDetails;
import com.logistics.orderservice.application.service.query.OrderQueryService;
import com.logistics.orderservice.domain.model.Order;
import com.logistics.orderservice.domain.repository.OrderRepository;
import com.logistics.orderservice.error.OrderErrorCode;
import com.logistics.orderservice.presentation.dto.response.OrderDetailResponse;
import com.logistics.orderservice.presentation.dto.response.OrderSummaryResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderQueryServiceTest {

    @Mock private OrderRepository orderRepository;

    private OrderQueryService orderQueryService;
    private UUID orderId;
    private UUID requesterId;
    private UUID hubId;
    private Order order;
    private CustomUserDetails requester;

    @BeforeEach
    void setUp() {
        orderQueryService = new OrderQueryService(orderRepository);
        orderId = UUID.randomUUID();
        requesterId = UUID.randomUUID();
        hubId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.of(2026, 8, 9, 10, 0);
        order = Order.create(
                "ORD-20260809-123456",
                requesterId,
                UUID.randomUUID(),
                hubId,
                "서울특별시 중구 세종대로 110",
                "홍길동",
                "hong.slack",
                "테스트 주문입니다.",
                now.plusDays(3),
                now
        );
        requester = user(requesterId, hubId, "COMPANY_MANAGER");
    }

    @Nested
    @DisplayName("주문 단건 조회")
    class GetOrderTest {

        @Test
        @DisplayName("주문자는 자신의 주문을 조회할 수 있다")
        void getOrder_requester_success() {
            given(orderRepository.findByIdAndDeletedAtIsNull(orderId))
                    .willReturn(Optional.of(order));

            OrderDetailResponse response = orderQueryService.getOrder(requester, orderId);

            assertThat(response.orderNumber()).isEqualTo("ORD-20260809-123456");
            assertThat(response.requesterId()).isEqualTo(requesterId);
            verify(orderRepository).findByIdAndDeletedAtIsNull(orderId);
        }

        @Test
        @DisplayName("MASTER는 다른 사용자의 주문도 조회할 수 있다")
        void getOrder_master_success() {
            CustomUserDetails master = user(UUID.randomUUID(), null, "MASTER");
            given(orderRepository.findByIdAndDeletedAtIsNull(orderId))
                    .willReturn(Optional.of(order));

            OrderDetailResponse response = orderQueryService.getOrder(master, orderId);

            assertThat(response.orderNumber()).isEqualTo("ORD-20260809-123456");
        }

        @Test
        @DisplayName("조회 권한이 없으면 ORDER_ACCESS_DENIED 예외가 발생한다")
        void getOrder_accessDenied() {
            CustomUserDetails otherUser = user(UUID.randomUUID(), UUID.randomUUID(), "COMPANY_MANAGER");
            given(orderRepository.findByIdAndDeletedAtIsNull(orderId))
                    .willReturn(Optional.of(order));

            assertThatThrownBy(() -> orderQueryService.getOrder(otherUser, orderId))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(exception -> assertThat(((BusinessException) exception).getErrorCode())
                            .isEqualTo(OrderErrorCode.ORDER_ACCESS_DENIED));
        }

        @Test
        @DisplayName("존재하지 않거나 삭제된 주문은 조회할 수 없다")
        void getOrder_notFound() {
            given(orderRepository.findByIdAndDeletedAtIsNull(orderId))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> orderQueryService.getOrder(requester, orderId))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(exception -> assertThat(((BusinessException) exception).getErrorCode())
                            .isEqualTo(OrderErrorCode.ORDER_NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("주문 목록 조회")
    class GetOrdersTest {

        private final Pageable pageable = PageRequest.of(0, 10);

        @Test
        @DisplayName("MASTER는 삭제되지 않은 전체 주문을 조회한다")
        void getOrders_master() {
            CustomUserDetails master = user(UUID.randomUUID(), null, "MASTER");
            given(orderRepository.findAllByDeletedAtIsNull(pageable)).willReturn(orderPage());

            Page<OrderSummaryResponse> response = orderQueryService.getOrders(master, pageable);

            assertThat(response.getContent()).hasSize(1);
            verify(orderRepository).findAllByDeletedAtIsNull(pageable);
            verify(orderRepository, never()).findAllByRequesterIdAndDeletedAtIsNull(master.getId(), pageable);
        }

        @Test
        @DisplayName("HUB_MANAGER는 담당 허브의 주문을 조회한다")
        void getOrders_hubManager() {
            CustomUserDetails hubManager = user(UUID.randomUUID(), hubId, "HUB_MANAGER");
            given(orderRepository.findAllByManagedHubIdAndDeletedAtIsNull(hubId, pageable))
                    .willReturn(orderPage());

            Page<OrderSummaryResponse> response = orderQueryService.getOrders(hubManager, pageable);

            assertThat(response.getContent()).hasSize(1);
            verify(orderRepository).findAllByManagedHubIdAndDeletedAtIsNull(hubId, pageable);
        }

        @Test
        @DisplayName("일반 사용자는 자신이 요청한 주문만 조회한다")
        void getOrders_requester() {
            given(orderRepository.findAllByRequesterIdAndDeletedAtIsNull(requesterId, pageable))
                    .willReturn(orderPage());

            Page<OrderSummaryResponse> response = orderQueryService.getOrders(requester, pageable);

            assertThat(response.getContent()).hasSize(1);
            verify(orderRepository).findAllByRequesterIdAndDeletedAtIsNull(requesterId, pageable);
        }

        @Test
        @DisplayName("조회할 주문이 없으면 빈 페이지를 반환한다")
        void getOrders_empty() {
            given(orderRepository.findAllByRequesterIdAndDeletedAtIsNull(requesterId, pageable))
                    .willReturn(Page.empty(pageable));

            Page<OrderSummaryResponse> response = orderQueryService.getOrders(requester, pageable);

            assertThat(response.getContent()).isEmpty();
            assertThat(response.getTotalElements()).isZero();
        }

        private Page<Order> orderPage() {
            return new PageImpl<>(List.of(order), pageable, 1);
        }
    }

    private CustomUserDetails user(UUID userId, UUID userHubId, String role) {
        return CustomUserDetails.from(userId, userHubId, UUID.randomUUID(), role);
    }
}
