package com.logistics.orderservice.application.service;

import com.logistics.common.exception.BusinessException;
import com.logistics.orderservice.domain.model.Order;
import com.logistics.orderservice.domain.repository.OrderRepository;
import com.logistics.orderservice.error.OrderErrorCode;
import com.logistics.orderservice.presentation.dto.response.OrderDetailResponse;
import com.logistics.orderservice.presentation.dto.response.OrderSummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderQueryService {
    private final OrderRepository orderRepository;


    public OrderDetailResponse getOrder(UUID orderId) {
        //일반 단건 조회
        Order order  = orderRepository.findByIdAndDeletedAtIsNull(orderId)
                .orElseThrow(() -> new BusinessException(OrderErrorCode.ORDER_NOT_FOUND));

        //권한 별로 조회할 수 있게

        return OrderDetailResponse.from(order);
    }

    public Page<OrderSummaryResponse> getOrders(Pageable pageable) {

        //일반 목록 조회
        Page<Order> orders = orderRepository.findAllByDeletedAtIsNull(pageable);

        //권한 별로 조회할 수 있게

       return orders.map(OrderSummaryResponse::from);
    }
}
