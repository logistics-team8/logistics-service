package com.logistics.deliveryservice.infrastructure.persistence;

import com.logistics.deliveryservice.domain.model.Delivery;
import com.logistics.deliveryservice.domain.model.DeliveryRouteHistory;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface DeliveryRouteHistoryJpaRepository extends JpaRepository<DeliveryRouteHistory, UUID> {

    List<DeliveryRouteHistory> findByDeliveryAndDeletedAtIsNullOrderBySequenceAsc(Delivery delivery);

    Optional<DeliveryRouteHistory> findByRouteIdAndDeliveryAndDeletedAtIsNull(
            UUID routeId,
            Delivery delivery
    );
}
