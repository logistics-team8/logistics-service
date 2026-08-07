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

    private static final String ROLE_MASTER = "ROLE_MASTER";
    private final OrderRepository orderRepository;


    /**
     * 주문 단건 조회
     *
     * 현재는 권한 검증 없이 삭제되지 않은 주문을 조회하도록
     * Security 구현이 완료되면 권한에 따른 접근 범위 추가 및 수정
     */
    public OrderDetailResponse getOrder(UUID orderId) {
        Order order = orderRepository
                .findByIdAndDeletedAtIsNull(orderId)
                .orElseThrow(() ->
                        new BusinessException(
                                OrderErrorCode.ORDER_NOT_FOUND
                        )
                );

        return OrderDetailResponse.from(order);
    }

    /**
     * 주문 목록 조회
     *
     * 현재는 권한 검증 없이 삭제되지 않은 주문 전체를 조회하도록
     * Security 구현이 완료되면 권한에 따른 접근 범위 추가 및 수정
     */
    public Page<OrderSummaryResponse> getOrders(
            Pageable pageable
    ) {
        return orderRepository
                .findAllByDeletedAtIsNull(pageable)
                .map(OrderSummaryResponse::from);
    }
}
