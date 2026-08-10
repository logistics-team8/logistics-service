package com.logistics.deliveryservice.domain.repository;

import com.logistics.deliveryservice.domain.model.DeliveryManager;
import com.logistics.deliveryservice.domain.model.DeliveryManagerAssignmentGroup;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 배송 담당자 등록과 배정에 필요한 영속성 기능을 애플리케이션 계층에 제공한다.
 */
public interface DeliveryManagerRepository {

    DeliveryManager save(DeliveryManager deliveryManager);

    Optional<DeliveryManager> findByUserId(UUID userId);

    List<Integer> findActiveDeliverySequences(DeliveryManagerAssignmentGroup assignmentGroup);
}
