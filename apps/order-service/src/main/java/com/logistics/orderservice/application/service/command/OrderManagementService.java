package com.logistics.orderservice.application.service.command;

import com.logistics.common.exception.BusinessException;
import com.logistics.common.security.principal.CustomUserDetails;
import com.logistics.orderservice.application.authorization.OrderAuthorization;
import com.logistics.orderservice.application.command.UpdateOrderCommand;
import com.logistics.orderservice.domain.model.Order;
import com.logistics.orderservice.domain.repository.OrderRepository;
import com.logistics.orderservice.error.OrderErrorCode;
import com.logistics.orderservice.presentation.dto.response.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderManagementService {
    private final OrderRepository orderRepository;
    private final OrderAuthorization orderAuthorization;
    private final Clock clock;

    /**
     * 주문 수정
     * 현재는 인증 및 권한 검사 없이 기본 수정 기능만 구현
     * 수정 가능 상태: PENDING
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

        orderAuthorization.validateOrderManagementPermission(user, order);

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

        orderAuthorization.validateOrderManagementPermission(user, order);

        order.delete(user.getId());
        return DeleteOrderResponse.from(order);
    }
}
