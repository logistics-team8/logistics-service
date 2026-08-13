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

    // 현재 활동 중인 배송 담당자들이 사용하고 있는 순번(번호표) 목록을 데이터베이스에서 찾음
    @Override
    public List<Integer> findActiveDeliverySequences(
            DeliveryManagerAssignmentGroup assignmentGroup
    ) {
        // 허브 배송원은 전역 조회, 업체 배송원은 소속 허브별로 격리하여 사용 중인 순번 목록 조회
        if (assignmentGroup.managerType() == DeliveryManagerType.HUB_DELIVERY) {
            return deliveryManagerJpaRepository.findActiveDeliverySequencesByManagerType(
                    DeliveryManagerType.HUB_DELIVERY
            );
        }
        return deliveryManagerJpaRepository.findActiveDeliverySequencesByManagerTypeAndHubId(
                DeliveryManagerType.COMPANY_DELIVERY,
                assignmentGroup.hubId()
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

    // 담당자 단건 조회(삭제되지 않은)
    @Override
    public Optional<DeliveryManager> findActiveByUserId(UUID userId) {
        return deliveryManagerJpaRepository.findByUserIdAndDeletedAtIsNull(userId);
    }

}
