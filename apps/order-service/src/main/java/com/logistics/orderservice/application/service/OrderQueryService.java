package com.logistics.orderservice.application.service;

import com.logistics.common.exception.BusinessException;
import com.logistics.common.security.principal.CustomUserDetails;
import com.logistics.orderservice.domain.model.Order;
import com.logistics.orderservice.domain.repository.OrderRepository;
import com.logistics.orderservice.error.OrderErrorCode;
import com.logistics.orderservice.presentation.dto.response.OrderDetailResponse;
import com.logistics.orderservice.presentation.dto.response.OrderSummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Transactional(readOnly = true)
@Service
@RequiredArgsConstructor
public class OrderQueryService {

    private static final String ROLE_MASTER = "ROLE_MASTER";
    private static final String ROLE_HUB_MANAGER = "ROLE_HUB_MANAGER";
    private final OrderRepository orderRepository;


    /**
     * 주문 단건 조회
     *
     * 마스터는 모든 주문 조회 가능
     * 허브 관리자는 자신이 담당하는 허브의 모든 주문 조회 가능
     * 로그인한 유저의 본인 주문 조회
     */
    public OrderDetailResponse getOrder(CustomUserDetails user, UUID orderId) {
        Order order = orderRepository
                .findByIdAndDeletedAtIsNull(orderId)
                .orElseThrow(() ->
                        new BusinessException(
                                OrderErrorCode.ORDER_NOT_FOUND
                        )
                );
        validateReadPermission(user, order);
        return OrderDetailResponse.from(order);
    }

    /**
     * 주문 목록 조회
     *
     * 마스터는 모든 주문 조회 가능
     * 허브 관리자는 자신이 담당하는 허브의 모든 주문 조회 가능
     * 로그인한 유저의 본인 주문 조회
     */
    public Page<OrderSummaryResponse> getOrders(CustomUserDetails user, Pageable pageable) {

        if(hasRole(user, ROLE_MASTER)){
            return orderRepository
                    .findAllByDeletedAtIsNull(pageable)
                    .map(OrderSummaryResponse::from);
        }

        if(hasRole(user, ROLE_HUB_MANAGER)){
            return orderRepository
                    .findAllByManagedHubIdAndDeletedAtIsNull(user.getHubId(), pageable)
                    .map(OrderSummaryResponse::from);
        }

        return orderRepository
                .findAllByRequesterIdAndDeletedAtIsNull(user.getId(),pageable)
                .map(OrderSummaryResponse::from);
    }



    /**
     * 주문 조회 권한 검사
     */
    private void validateReadPermission(CustomUserDetails user, Order order) {
        if(hasRole(user, ROLE_MASTER)){
            return;
        }
        if (hasRole(user, ROLE_HUB_MANAGER) && isManagedHubOrder(user, order)) {
            return;
        }
        //본인 주문에 대한 조회가능
        if (user.getId().equals(order.getRequesterId())) {
            return;
        }
        throw new BusinessException(OrderErrorCode.ORDER_ACCESS_DENIED);
    }



    /**
     *  로그인한 허브 매니저가 해당 주문을 담당하는 허브관리자인지 확인한다.
     */
    private boolean isManagedHubOrder(CustomUserDetails user, Order order) {
        UUID userHubId = user.getHubId();

        boolean destinationHubOrder =
                order.getDestinationHubId() != null && userHubId.equals(order.getDestinationHubId());
        if(destinationHubOrder){
            return true;
        }

        return order.getOrderItems().stream()
                .anyMatch(orderItem ->
                        orderItem.getDepartureHubId() != null && userHubId.equals(orderItem.getDepartureHubId()
                        )
                );
    }

    private boolean hasRole(CustomUserDetails user, String role) {
        return user.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role::equals);
    }
}
