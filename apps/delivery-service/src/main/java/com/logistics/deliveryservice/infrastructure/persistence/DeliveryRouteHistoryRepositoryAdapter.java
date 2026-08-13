package com.logistics.deliveryservice.infrastructure.persistence;

import com.logistics.deliveryservice.domain.model.Delivery;
import com.logistics.deliveryservice.domain.model.DeliveryRouteHistory;
import com.logistics.deliveryservice.domain.repository.DeliveryRouteHistoryRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * 경로 이력 조회를 Spring Data JPA에 위임
 */
@Repository
@RequiredArgsConstructor
public class DeliveryRouteHistoryRepositoryAdapter implements DeliveryRouteHistoryRepository {

    private final DeliveryRouteHistoryJpaRepository deliveryRouteHistoryJpaRepository;

    @Override
    public List<DeliveryRouteHistory> findActiveByDeliveryOrderBySequence(Delivery delivery) {
        return deliveryRouteHistoryJpaRepository
                .findByDeliveryAndDeletedAtIsNullOrderBySequenceAsc(delivery);
    }
}
