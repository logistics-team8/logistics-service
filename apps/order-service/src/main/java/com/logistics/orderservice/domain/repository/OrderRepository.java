package com.logistics.orderservice.domain.repository;

import com.logistics.orderservice.domain.model.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OrderRepository{
    Order save(Order order);



    /**
     * 마스터 권한의 단건, 목록 조회
     */
    Optional<Order> findByIdAndDeletedAtIsNull(UUID orderId);

    Page<Order> findAllByDeletedAtIsNull(Pageable pageable);



    /**
     * 로그인한 사용자의 주문 조회
     */
    Optional<Order> findByIdAndRequesterIdAndDeletedAtIsNull(UUID orderId, UUID userId);

    Page<Order> findAllByRequesterIdAndDeletedAtIsNull(UUID userId, Pageable pageable);
}
