package com.logistics.deliveryservice.domain.repository;

import com.logistics.deliveryservice.domain.model.Delivery;
import com.logistics.deliveryservice.domain.model.DeliveryStatus;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Delivery Aggregate의 영속성 기능을 애플리케이션 계층에 제공한다.
 */
public interface DeliveryRepository {

    Delivery save(Delivery delivery);

    Optional<Delivery> findByOrderId(UUID orderId);

    Optional<Delivery> findActiveByOrderId(UUID orderId);

    Optional<Delivery> findActiveByDeliveryId(UUID deliveryId);

    Page<Delivery> search(
            DeliveryStatus status,
            UUID orderId,
            UUID hubId,
            UUID deliveryManagerId,
            Pageable pageable
    );

    boolean existsActiveManagerAssignment(
            UUID managerUserId,
            Collection<DeliveryStatus> deliveryStatuses
    );
}
