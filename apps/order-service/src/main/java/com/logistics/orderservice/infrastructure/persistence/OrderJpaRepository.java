package com.logistics.orderservice.infrastructure.persistence;

import com.logistics.orderservice.domain.model.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OrderJpaRepository extends JpaRepository<Order, UUID> {

    Optional<Order> findByIdAndDeletedAtIsNull(UUID orderId);

    Page<Order> findAllByDeletedAtIsNull(Pageable pageable);


    Optional<Order> findByIdAndRequesterIdAndDeletedAtIsNull(UUID orderId, UUID userId);

    Page<Order> findAllByRequesterIdAndDeletedAtIsNull(UUID userId, Pageable pageable);

}
