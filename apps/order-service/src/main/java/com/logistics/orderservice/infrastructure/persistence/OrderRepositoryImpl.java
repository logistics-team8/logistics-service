package com.logistics.orderservice.infrastructure.persistence;

import com.logistics.orderservice.domain.model.Order;
import com.logistics.orderservice.domain.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;


@Repository
@RequiredArgsConstructor
public class OrderRepositoryImpl implements OrderRepository {

    private final OrderJpaRepository orderJpaRepository;

    @Override
    public Order save(Order order) {
        return orderJpaRepository.save(order);
    }

    @Override
    public Optional<Order> findByIdAndDeletedAtIsNull(UUID orderId) {
        return orderJpaRepository
                .findByIdAndDeletedAtIsNull(orderId);
    }

    @Override
    public Page<Order> findAllByDeletedAtIsNull(Pageable pageable) {
        return orderJpaRepository
                .findAllByDeletedAtIsNull(pageable);
    }



}
