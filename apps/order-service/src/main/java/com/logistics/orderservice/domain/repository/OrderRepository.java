package com.logistics.orderservice.domain.repository;

import com.logistics.orderservice.domain.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OrderRepository{
    Order save(Order order);
}
