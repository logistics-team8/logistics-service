package com.logistics.deliveryservice.domain.repository;

import com.logistics.deliveryservice.domain.model.DeliveryManager;
import com.logistics.deliveryservice.domain.model.DeliveryManagerAssignmentGroup;
import com.logistics.deliveryservice.domain.model.DeliveryManagerType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

// 도메인 계층에서 필요한 저장 기능의 목록만 정의
public interface DeliveryManagerRepository {

    // 담당자 저장
    DeliveryManager save(DeliveryManager deliveryManager);

    // UserId로 조회
    Optional<DeliveryManager> findByUserId(UUID userId);

    // 논리 삭제되지 않은 담당자 단건 조회
    Optional<DeliveryManager> findActiveByUserId(UUID userId);

    // 사용 중인 순번 조회
    List<Integer> findActiveDeliverySequences(DeliveryManagerAssignmentGroup assignmentGroup);

    // 목록 검색
    Page<DeliveryManager> search(
            UUID hubId,
            DeliveryManagerType managerType,
            Pageable pageable
    );
}
