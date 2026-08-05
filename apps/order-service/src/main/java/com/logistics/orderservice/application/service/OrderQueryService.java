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
     * 마스터 권한을 가진 사용자와 타 사용자의 조회 권한 분리
     * 추후에 userDetails에서 role과 id 가져울 것임
     * @param orderId
     * @param userId
     * @param role
     * @return
     */
    public OrderDetailResponse getOrder(UUID orderId, UUID userId, String role) {

        Order order;

        //마스터 권한의 단건 조회
        if(ROLE_MASTER.equals(role)){
            order  = orderRepository.findByIdAndDeletedAtIsNull(orderId)
                    .orElseThrow(() -> new BusinessException(OrderErrorCode.ORDER_NOT_FOUND));
        }else{
            //로그인한 사용자가 주문한 주문 조회
            order = orderRepository.findByIdAndRequesterIdAndDeletedAtIsNull(orderId, userId)
                    .orElseThrow(() -> new BusinessException(OrderErrorCode.ORDER_NOT_FOUND));
        }

        return OrderDetailResponse.from(order);
    }


    /**
     * 마스터 권한을 가진 사용자와 타 사용자의 목록 조회 권한 분리
     * 추후에 userDetails에서 role과 id 가져울 것임
     * @param pageable
     * @param userId
     * @param role
     * @return
     */
    public Page<OrderSummaryResponse> getOrders(Pageable pageable, UUID userId, String role) {

        Page<Order> orders;

        //마스터 권한의 목록 조회
        if(ROLE_MASTER.equals(role)){
            orders = orderRepository.findAllByDeletedAtIsNull(pageable);
        }else{
            //로그인한 사용자가 주문한 주문 조회
            orders = orderRepository.findAllByRequesterIdAndDeletedAtIsNull(userId, pageable);
        }

       return orders.map(OrderSummaryResponse::from);
    }
}
