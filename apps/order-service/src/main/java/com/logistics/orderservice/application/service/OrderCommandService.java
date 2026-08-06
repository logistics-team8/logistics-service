package com.logistics.orderservice.application.service;

import com.logistics.common.exception.BusinessException;
import com.logistics.orderservice.application.command.CreateOrderCommand;
import com.logistics.orderservice.application.command.CreateOrderItemCommand;
import com.logistics.orderservice.application.command.UpdateOrderCommand;
import com.logistics.orderservice.domain.model.Order;
import com.logistics.orderservice.domain.repository.OrderRepository;
import com.logistics.orderservice.error.OrderErrorCode;
import com.logistics.orderservice.presentation.dto.response.CreateOrderResponse;
import com.logistics.orderservice.presentation.dto.response.DeleteOrderResponse;
import com.logistics.orderservice.presentation.dto.response.UpdateOrderResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderCommandService {

    private final OrderRepository orderRepository;


    @Transactional
    public CreateOrderResponse createOrder(CreateOrderCommand command) {
        Order order = Order.create(
                generateOrderNumber(),
                command.requesterId(),
                command.receiverCompanyId(),
                command.requestMessage(),
                command.requestedDeliveryAt()
        );

        for (CreateOrderItemCommand item : command.items()) {
            order.addOrderItem(
                    item.productId(),
                    item.quantity()
            );
        }

        return CreateOrderResponse.from(orderRepository.save(order));
    }



    private String generateOrderNumber() {
        String date = LocalDate.now()
                .format(DateTimeFormatter.BASIC_ISO_DATE);

        //ex)ORD-20260804-A12F45C98D01
        String suffix = UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 12)
                .toUpperCase();

        return "ORD-" + date + "-" + suffix;
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
    public UpdateOrderResponse updateOrder(UUID userId, UpdateOrderCommand command, UUID orderId) {
        //user의 role을 확인하고 수정할 권한과 범위를 체크해야한다.


        Order order = orderRepository
                .findByIdAndDeletedAtIsNull(orderId)
                .orElseThrow(() ->
                    new BusinessException(OrderErrorCode.ORDER_NOT_FOUND)
                );

        order.update(
                command.requestMessage(),
                command.requestDeliveryAt()
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
    public DeleteOrderResponse deleteOrder(UUID userId, UUID orderId) {
        //user의 role을 확인하고 삭제할 권한과 범위를 체크해야한다.

        Order order = orderRepository
                .findByIdAndDeletedAtIsNull(orderId)
                .orElseThrow(() ->
                        new BusinessException(OrderErrorCode.ORDER_NOT_FOUND)
                );

        order.delete(userId);
        return DeleteOrderResponse.from(order);
    }
}
