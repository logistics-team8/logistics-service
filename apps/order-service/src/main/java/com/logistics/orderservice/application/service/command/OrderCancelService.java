package com.logistics.orderservice.application.service.command;

import com.logistics.common.exception.BusinessException;
import com.logistics.common.security.principal.CustomUserDetails;
import com.logistics.orderservice.application.authorization.OrderAuthorization;
import com.logistics.orderservice.application.port.ProductPort;
import com.logistics.orderservice.domain.model.Order;
import com.logistics.orderservice.domain.model.OrderItem;
import com.logistics.orderservice.domain.repository.OrderRepository;
import com.logistics.orderservice.error.OrderErrorCode;
import com.logistics.orderservice.presentation.dto.response.CancelOrderItemResponse;
import com.logistics.orderservice.presentation.dto.response.CancelOrderResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderCancelService {

    private final OrderRepository orderRepository;
    private final OrderAuthorization orderAuthorization;
    private final ProductPort productPort;
    private final OrderStateService orderStateService;
    private final Clock clock;

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

        orderAuthorization.validateCancelPermission(user, order);

        List<ProductPort.StockItem> restoreItems =
                order.getOrderItems()
                        .stream()
                        .filter(orderItem -> !orderItem.isCanceled()
                        )
                        .map(orderItem ->
                                new ProductPort.StockItem(
                                        orderItem.getProductId(),
                                        orderItem.getQuantity()
                                )
                        )
                        .toList();

        if(order.requiresStockRestoreForCancel()){
            productPort.restoreStock(restoreItems);
        }

        LocalDateTime now = LocalDateTime.now(clock);
        Order canceledOrder = orderStateService.cancelOrder(orderId, user.getId(), now);

        return CancelOrderResponse.from(canceledOrder);
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

        orderAuthorization.validateCancelPermission(user, order);

        OrderItem canceledOrderItem = order.cancelOrderItem(orderItemId, user.getId());
        return CancelOrderItemResponse.from(order,canceledOrderItem);
    }
}
