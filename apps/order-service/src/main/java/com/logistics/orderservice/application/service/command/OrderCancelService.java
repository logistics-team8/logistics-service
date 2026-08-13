package com.logistics.orderservice.application.service.command;

import com.logistics.common.exception.BusinessException;
import com.logistics.common.security.principal.CustomUserDetails;
import com.logistics.orderservice.application.authorization.OrderAuthorization;
import com.logistics.orderservice.application.exception.StockRestoreException;
import com.logistics.orderservice.application.exception.StockRestoreUnknownException;
import com.logistics.orderservice.application.port.ProductPort;
import com.logistics.orderservice.domain.model.Order;
import com.logistics.orderservice.domain.model.OrderItem;
import com.logistics.orderservice.domain.repository.OrderRepository;
import com.logistics.orderservice.error.OrderErrorCode;
import com.logistics.orderservice.presentation.dto.response.CancelOrderItemResponse;
import com.logistics.orderservice.presentation.dto.response.CancelOrderResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderCancelService {

    private final OrderRepository orderRepository;
    private final OrderAuthorization orderAuthorization;
    private final StockProcessService stockProcessService;
    private final OrderStateService orderStateService;
    private final Clock clock;

    /**
     * 주문 취소
     */
    public CancelOrderResponse cancelOrder(CustomUserDetails user, UUID orderId) {
        Order order = orderRepository
                .findByIdAndDeletedAtIsNull(orderId)
                .orElseThrow(() ->
                        new BusinessException(OrderErrorCode.ORDER_NOT_FOUND)
                );

        orderAuthorization.validateCancelPermission(user, order);
        order.validateCancelable();



        if(order.requiresStockRestoreForCancel()){
            List<ProductPort.StockItem> restoreItems = restoreItems(order);
            stockProcessService.restoreStockForCancel(orderId, restoreItems);
        }

        LocalDateTime now = LocalDateTime.now(clock);
        Order canceledOrder = orderStateService.cancelOrder(orderId, user.getId(), now);

        return CancelOrderResponse.from(canceledOrder);
    }


    /**
     * 주문 상품 취소
     * 상품 개별 취소는 미구현
     */
    @Transactional
    public CancelOrderItemResponse cancelOrderItem(CustomUserDetails user, UUID orderId, UUID orderItemId) {
        Order order = orderRepository
                .findByIdAndDeletedAtIsNull(orderId)
                .orElseThrow(() ->
                        new BusinessException(OrderErrorCode.ORDER_NOT_FOUND)
                );

        orderAuthorization.validateCancelPermission(user, order);

        LocalDateTime now = LocalDateTime.now(clock);
        OrderItem canceledOrderItem = order.cancelOrderItem(orderItemId, user.getId(), now);
        return CancelOrderItemResponse.from(order,canceledOrderItem);
    }


    private List<ProductPort.StockItem> restoreItems(Order order) {

             return order.getOrderItems()
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
    }


}
