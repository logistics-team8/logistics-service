package com.logistics.deliveryservice.infrastructure.persistence;

import com.logistics.deliveryservice.domain.model.Delivery;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface DeliveryJpaRepository extends JpaRepository<Delivery, UUID> {
}
