package com.logistics.orderservice.application.service;

import com.logistics.common.exception.BusinessException;
import com.logistics.common.security.principal.CustomUserDetails;
import com.logistics.orderservice.application.command.CreateOrderCommand;
import com.logistics.orderservice.application.command.CreateOrderItemCommand;
import com.logistics.orderservice.application.command.UpdateOrderCommand;
import com.logistics.orderservice.domain.model.Order;
import com.logistics.orderservice.domain.model.OrderItem;
import com.logistics.orderservice.domain.repository.OrderRepository;
import com.logistics.orderservice.error.OrderErrorCode;
import com.logistics.orderservice.presentation.dto.response.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderCommandService {

    private static final String ROLE_MASTER = "ROLE_MASTER";
    private static final String ROLE_HUB_MANAGER = "ROLE_HUB_MANAGER";


    private final OrderRepository orderRepository;
    private final Clock clock;


    @Transactional
    public CreateOrderResponse createOrder(CreateOrderCommand command, CustomUserDetails user) {
        LocalDateTime now = LocalDateTime.now(clock);

        Order order = Order.create(
                generateOrderNumber(now),
                user.getId(),
                command.receiverCompanyId(),
                command.requestMessage(),
                command.requestedDeliveryAt(),
                now
        );

        for (CreateOrderItemCommand item : command.items()) {
            order.addOrderItem(
                    item.productId(),
                    item.quantity()
            );
        }

        return CreateOrderResponse.from(orderRepository.save(order));
    }

    /**
     * 주문 수정
     * 현재는 인증 및 권한 검사 없이 기본 수정 기능만 구현
     * 수정 가능 상태: PENDING
     * Security 적용 후:
     * - MASTER: 모든 주문 수정
     * - HUB_MANAGER: 담당 허브의 주문만 수정 가능
     */
    @Transactional
    public UpdateOrderResponse updateOrder(CustomUserDetails user, UpdateOrderCommand command, UUID orderId) {
        //user의 role을 확인하고 수정할 권한과 범위를 체크해야한다.

        Order order = orderRepository
                .findByIdAndDeletedAtIsNull(orderId)
                .orElseThrow(() ->
                        new BusinessException(OrderErrorCode.ORDER_NOT_FOUND)
                );

        validateOrderManagementPermission(user, order);

        LocalDateTime now = LocalDateTime.now(clock);
        order.update(
                command.requestMessage(),
                command.requestedDeliveryAt(),
                now
        );

        return UpdateOrderResponse.from(order);
    }


    /**
     * 주문 논리 삭제
     * 현재는 인증 및 권한 검사 없이 기본 삭제 기능만 구현
     * 삭제 가능 상태: FAILED, CANCELED
     * Security 적용 후:
     * - MASTER: 모든 주문 삭제
     * - HUB_MANAGER: 담당 허브의 주문 삭제
     */
    @Transactional
    public DeleteOrderResponse deleteOrder(CustomUserDetails user, UUID orderId) {
        //user의 role을 확인하고 삭제할 권한과 범위를 체크해야한다.

        Order order = orderRepository
                .findByIdAndDeletedAtIsNull(orderId)
                .orElseThrow(() ->
                        new BusinessException(OrderErrorCode.ORDER_NOT_FOUND)
                );

        validateOrderManagementPermission(user, order);

        order.delete(user.getId());
        return DeleteOrderResponse.from(order);
    }


    /**
     * 주문 취소
     */
    @Transactional
    public CancelOrderResponse cancelOrder(CustomUserDetails user, UUID orderId) {
        Order order = orderRepository
                .findByIdAndDeletedAtIsNull(orderId)
                .orElseThrow(() ->
                        new BusinessException(OrderErrorCode.ORDER_NOT_FOUND)
                );

        validateCancelPermission(user, order);

        order.cancel(user.getId());
        return CancelOrderResponse.from(order);
    }


    /**
     * 주문 상품 취소
     */
    @Transactional
    public CancelOrderItemResponse cancelOrderItem(CustomUserDetails user, UUID orderId, UUID orderItemId) {
        Order order = orderRepository
                .findByIdAndDeletedAtIsNull(orderId)
                .orElseThrow(() ->
                        new BusinessException(OrderErrorCode.ORDER_NOT_FOUND)
                );

        validateCancelPermission(user, order);

        OrderItem canceledOrderItem = order.cancelOrderItem(orderItemId, user.getId());
        return CancelOrderItemResponse.from(order,canceledOrderItem);
    }


    /**
     * 주문 번호 생성 메서드
     * //ex)ORD-20260804-A12F45C98D01
     */
    private String generateOrderNumber(LocalDateTime now) {
        String date = now.toLocalDate()
                .format(DateTimeFormatter.BASIC_ISO_DATE);


        String suffix = UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 12)
                .toUpperCase();

        return "ORD-" + date + "-" + suffix;
    }


    /**
     * 주문 수정/삭제 권한 검사
     */
    private void validateOrderManagementPermission(CustomUserDetails user, Order order) {
        if(hasRole(user, ROLE_MASTER)){
            return;
        }
        if (hasRole(user, ROLE_HUB_MANAGER) && isManagedHubOrder(user, order)) {
            return;
        }
        throw new BusinessException(OrderErrorCode.ORDER_ACCESS_DENIED);
    }


    /**
     * 주문 취소 권한 검사
     */
    private void validateCancelPermission(CustomUserDetails user, Order order) {
        if(hasRole(user, ROLE_MASTER)){
            return;
        }
        if (hasRole(user, ROLE_HUB_MANAGER) && isManagedHubOrder(user, order)) {
            return;
        }
        //본인 주문에 대한 취소 가능
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
