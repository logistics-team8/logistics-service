package com.logistics.deliveryservice.infrastructure.persistence;

import com.logistics.deliveryservice.domain.model.Delivery;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

interface DeliveryJpaRepository extends JpaRepository<Delivery, UUID> {

    @EntityGraph(attributePaths = "routeHistories")
    Optional<Delivery> findByOrderId(UUID orderId);

    @EntityGraph(attributePaths = "routeHistories")
    Optional<Delivery> findByOrderIdAndDeletedAtIsNull(UUID orderId);
}
