package com.logistics.deliveryservice.domain.repository;

import com.logistics.deliveryservice.domain.model.DeliveryManager;
import com.logistics.deliveryservice.domain.model.DeliveryManagerAssignmentGroup;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

// 도메인 계층에서 필요한 저장 기능의 목록만 정의
public interface DeliveryManagerRepository {

    // 담당자 저장
    DeliveryManager save(DeliveryManager deliveryManager);

    // UserId로 조회
    Optional<DeliveryManager> findByUserId(UUID userId);

    // 사용 중인 순번 조회
    List<Integer> findActiveDeliverySequences(DeliveryManagerAssignmentGroup assignmentGroup);
}
