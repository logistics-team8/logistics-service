package com.logistics.deliveryservice.infrastructure.persistence;

import com.logistics.deliveryservice.domain.model.Delivery;
import com.logistics.deliveryservice.domain.model.DeliveryRouteHistory;
import com.logistics.deliveryservice.domain.repository.DeliveryRouteHistoryRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
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
    public DeliveryRouteHistory save(DeliveryRouteHistory routeHistory) {
        return deliveryRouteHistoryJpaRepository.saveAndFlush(routeHistory);
    }

    @Override
    public List<DeliveryRouteHistory> findActiveByDeliveryOrderBySequence(Delivery delivery) {
        return deliveryRouteHistoryJpaRepository
                .findByDeliveryAndDeletedAtIsNullOrderBySequenceAsc(delivery);
    }

    @Override
    public Optional<DeliveryRouteHistory> findActiveByRouteIdAndDelivery(
            UUID routeId,
            Delivery delivery
    ) {
        return deliveryRouteHistoryJpaRepository
                .findByRouteIdAndDeliveryAndDeletedAtIsNull(routeId, delivery);
    }
}
