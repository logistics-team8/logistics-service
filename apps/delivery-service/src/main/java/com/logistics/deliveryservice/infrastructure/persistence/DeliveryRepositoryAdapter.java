package com.logistics.deliveryservice.infrastructure.persistence;

import com.logistics.deliveryservice.domain.model.Delivery;
import com.logistics.deliveryservice.domain.repository.DeliveryRepository;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * 도메인 Repository 요청을 Spring Data JPA에 위임한다.
 */
@Repository
@RequiredArgsConstructor
public class DeliveryRepositoryAdapter implements DeliveryRepository {

    private final DeliveryJpaRepository deliveryJpaRepository;

    @Override
    public Delivery save(Delivery delivery) {
        // order_id Unique 경합을 서비스 호출 안에서 판별할 수 있도록 즉시 flush한다.
        return deliveryJpaRepository.saveAndFlush(delivery);
    }

    @Override
    public Optional<Delivery> findByOrderId(UUID orderId) {
        return deliveryJpaRepository.findByOrderId(orderId);
    }

    @Override
    public Optional<Delivery> findActiveByOrderId(UUID orderId) {
        return deliveryJpaRepository.findByOrderIdAndDeletedAtIsNull(orderId);
    }
}
