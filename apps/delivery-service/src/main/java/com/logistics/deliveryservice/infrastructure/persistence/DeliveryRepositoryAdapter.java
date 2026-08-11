package com.logistics.deliveryservice.infrastructure.persistence;

import com.logistics.deliveryservice.domain.model.Delivery;
import com.logistics.deliveryservice.domain.repository.DeliveryRepository;
import org.springframework.stereotype.Repository;

/**
 * 도메인 Repository 요청을 Spring Data JPA에 위임한다.
 */
@Repository
public class DeliveryRepositoryAdapter implements DeliveryRepository {

    private final DeliveryJpaRepository deliveryJpaRepository;

    public DeliveryRepositoryAdapter(DeliveryJpaRepository deliveryJpaRepository) {
        this.deliveryJpaRepository = deliveryJpaRepository;
    }

    @Override
    public Delivery save(Delivery delivery) {
        return deliveryJpaRepository.save(delivery);
    }
}
