package com.logistics.deliveryservice.domain.repository;

import com.logistics.deliveryservice.domain.model.Delivery;
import java.util.Optional;
import java.util.UUID;

/**
 * Delivery Aggregate의 영속성 기능을 애플리케이션 계층에 제공한다.
 */
public interface DeliveryRepository {

    Delivery save(Delivery delivery);

    Optional<Delivery> findByOrderId(UUID orderId);

    Optional<Delivery> findActiveByOrderId(UUID orderId);
}
