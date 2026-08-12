package com.logistics.deliveryservice.infrastructure.persistence;

import com.logistics.deliveryservice.domain.model.DeliveryManager;
import com.logistics.deliveryservice.domain.model.DeliveryManagerAssignmentGroup;
import com.logistics.deliveryservice.domain.model.DeliveryManagerType;
import com.logistics.deliveryservice.domain.repository.DeliveryManagerRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

// 도메인 Repository와 Spring Data JPA 사이를 연결
@Repository
@RequiredArgsConstructor
public class DeliveryManagerRepositoryAdapter implements DeliveryManagerRepository {

    private final DeliveryManagerJpaRepository deliveryManagerJpaRepository;

    @Override
    public DeliveryManager save(DeliveryManager deliveryManager) {
        // 동시에 같은 아이디로 가입할 때 생기는 중복 에러를 바로 잡아내기 위해, DB에 즉시 반영(flush)
        return deliveryManagerJpaRepository.saveAndFlush(deliveryManager);
    }

    @Override
    public Optional<DeliveryManager> findByUserId(UUID userId) {
        // 삭제된 담당자의 복구 여부도 판단해야 하므로 PK 조회에서는 삭제 조건을 적용하지 않는다.
        return deliveryManagerJpaRepository.findById(userId);
    }

    // 배정 그룹의 타입과 허브 ID를 JPA Repository에 전달
    @Override
    public List<Integer> findActiveDeliverySequences(
            DeliveryManagerAssignmentGroup assignmentGroup
    ) {
        return deliveryManagerJpaRepository.findActiveDeliverySequences(
                assignmentGroup.managerType(),
                assignmentGroup.hubId()
        );
    }

    @Override
    public List<DeliveryManager> findActiveManagers(
            DeliveryManagerAssignmentGroup assignmentGroup
    ) {
        // 허브 배송은 전역 그룹이므로 hubId 필터를 생략한다.
        UUID hubIdFilter = assignmentGroup.managerType() == DeliveryManagerType.HUB_DELIVERY
                ? null
                : assignmentGroup.hubId();
        return deliveryManagerJpaRepository.findActiveManagers(
                assignmentGroup.managerType(),
                hubIdFilter
        );
    }

    // 담당자 목록 조회
    @Override
    public Page<DeliveryManager> search(
            UUID hubId,
            DeliveryManagerType managerType,
            Pageable pageable
    ) {
        return deliveryManagerJpaRepository.search(hubId, managerType, pageable);
    }
}
