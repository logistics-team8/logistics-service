package com.logistics.orderservice.application.service.command;

import com.logistics.common.exception.BusinessException;
import com.logistics.orderservice.domain.model.Order;
import com.logistics.orderservice.domain.model.OrderFailureReason;
import com.logistics.orderservice.domain.repository.OrderRepository;
import com.logistics.orderservice.error.OrderErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderStateService {
    private final OrderRepository orderRepository;

    //주문 생성
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Order createPendingOrder(Order order){
        return orderRepository.save(order);
    }

    //재고 차감 성공
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Order confirmOrder(UUID orderId){
        Order order = getOrder(orderId);
        order.confirm();
        return order;
    }


    //재고 차감 결과 확인 불가
    //상태는 PENDING을 유지
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Order markStockDecreaseUnknown(UUID orderId){
        Order order = getOrder(orderId);
        order.markStockDecreaseUnknown();
        return order;
    }

    //Delivery 생성 성공
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Order markDeliveryCreated(UUID orderId){
        Order order = getOrder(orderId);
        order.markDeliveryCreated();
        return order;
    }


    //Delivery 상태 확인 실패
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Order markDeliveryStatusCheckFailed(UUID orderId){
        Order order = getOrder(orderId);
        order.markDeliveryStatusCheckFailed();
        return order;
    }


    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void failOrder(UUID orderId, OrderFailureReason failureReason){
        Order order = getOrder(orderId);
        order.fail(failureReason);
    }


    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Order cancelOrder(UUID orderId, UUID canceledBy, LocalDateTime canceledAt){
        Order order = getOrder(orderId);
        order.cancel(canceledBy, canceledAt);
        return order;
    }

    private Order getOrder(UUID orderId){
        return orderRepository.findByIdAndDeletedAtIsNull(orderId)
                .orElseThrow(
                        () -> new BusinessException(OrderErrorCode.ORDER_NOT_FOUND)
                );
    }
}
