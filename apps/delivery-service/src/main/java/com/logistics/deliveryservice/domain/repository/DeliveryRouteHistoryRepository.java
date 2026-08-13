package com.logistics.deliveryservice.domain.repository;

import com.logistics.deliveryservice.domain.model.Delivery;
import com.logistics.deliveryservice.domain.model.DeliveryRouteHistory;
import java.util.List;

// 활성 Delivery에 속한 경로 이력을 조회
public interface DeliveryRouteHistoryRepository {

    List<DeliveryRouteHistory> findActiveByDeliveryOrderBySequence(Delivery delivery);
}
