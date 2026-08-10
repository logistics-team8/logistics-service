package com.logistics.deliveryservice.infrastructure.persistence;

import com.logistics.deliveryservice.domain.model.DeliveryManager;
import com.logistics.deliveryservice.domain.model.DeliveryManagerAssignmentGroup;
import com.logistics.deliveryservice.domain.repository.DeliveryManagerRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * 배송 담당자 도메인 Repository 요청을 Spring Data JPA에 위임한다.
 */
@Repository
@RequiredArgsConstructor
public class DeliveryManagerRepositoryAdapter implements DeliveryManagerRepository {

    private final DeliveryManagerJpaRepository deliveryManagerJpaRepository;

    @Override
    public DeliveryManager save(DeliveryManager deliveryManager) {
        // User ID 중복 경합을 등록 서비스가 같은 트랜잭션 안에서 판단할 수 있도록 즉시 flush한다.
        return deliveryManagerJpaRepository.saveAndFlush(deliveryManager);
    }

    @Override
    public Optional<DeliveryManager> findByUserId(UUID userId) {
        // 삭제된 담당자의 복구 여부도 판단해야 하므로 PK 조회에서는 삭제 조건을 적용하지 않는다.
        return deliveryManagerJpaRepository.findById(userId);
    }

    @Override
    public List<Integer> findActiveDeliverySequences(
            DeliveryManagerAssignmentGroup assignmentGroup
    ) {
        return deliveryManagerJpaRepository.findActiveDeliverySequences(
                assignmentGroup.managerType(),
                assignmentGroup.hubId()
        );
    }
}
