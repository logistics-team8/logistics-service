package com.logistics.deliveryservice.domain.repository;

import com.logistics.deliveryservice.domain.model.Delivery;
import com.logistics.deliveryservice.domain.model.DeliveryRouteHistory;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

// 활성 Delivery에 속한 경로 이력을 조회
public interface DeliveryRouteHistoryRepository {

    DeliveryRouteHistory save(DeliveryRouteHistory routeHistory);

    List<DeliveryRouteHistory> findActiveByDeliveryOrderBySequence(Delivery delivery);

    Optional<DeliveryRouteHistory> findActiveByRouteIdAndDelivery(
            UUID routeId,
            Delivery delivery
    );
}
