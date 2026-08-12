package com.logistics.deliveryservice.infrastructure.persistence;

import com.logistics.deliveryservice.domain.model.DeliveryManagerAssignmentCursor;
import com.logistics.deliveryservice.domain.model.DeliveryManagerAssignmentGroup;
import com.logistics.deliveryservice.domain.repository.DeliveryManagerAssignmentCursorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 배정 그룹 Cursor를 준비하고 비관적 쓰기 잠금으로 반환한다.
 */
@Repository
@RequiredArgsConstructor
public class DeliveryManagerAssignmentCursorRepositoryAdapter
        implements DeliveryManagerAssignmentCursorRepository {

    private final DeliveryManagerAssignmentCursorJpaRepository cursorJpaRepository;

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public DeliveryManagerAssignmentCursor acquireForUpdate(
            DeliveryManagerAssignmentGroup assignmentGroup
    ) {
        DeliveryManagerAssignmentCursor newCursor =
                DeliveryManagerAssignmentCursor.create(assignmentGroup);

        // 최초 등록이 동시에 실행되어도 그룹 키 Unique 제약으로 Cursor 행은 하나만 만든다.
        cursorJpaRepository.insertIfAbsent(
                newCursor.getCursorId(),
                newCursor.getAssignmentGroupKey(),
                newCursor.getManagerType().name(),
                newCursor.getHubId()
        );

        // 외부 Application Service 트랜잭션이 끝날 때까지 잠금을 유지해 빈 순번 선택을 직렬화한다.
        return cursorJpaRepository.findByAssignmentGroupKeyForUpdate(
                        assignmentGroup.assignmentGroupKey()
                )
                .orElseThrow(() -> new IllegalStateException(
                        "배정 그룹 Cursor를 생성하거나 조회할 수 없습니다."
                ));
    }
}
