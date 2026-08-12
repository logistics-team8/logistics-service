package com.logistics.deliveryservice.domain.model;

import com.logistics.deliveryservice.domain.common.BaseEntity;
import com.logistics.deliveryservice.domain.exception.DeliveryErrorCode;
import com.logistics.deliveryservice.domain.exception.DeliveryException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 배정 그룹별 잠금 기준과 마지막 자동 배정 순번을 보관한다.
 */
@Getter
@Entity
@Table(
        name = "p_delivery_manager_assignment_cursors",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_delivery_manager_assignment_cursors_group_key",
                columnNames = "assignment_group_key"
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeliveryManagerAssignmentCursor extends BaseEntity {

    @Id
    @Column(name = "cursor_id", nullable = false, updatable = false)
    private UUID cursorId;

    @Column(name = "assignment_group_key", nullable = false, updatable = false, length = 80)
    private String assignmentGroupKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "manager_type", nullable = false, updatable = false, length = 30)
    private DeliveryManagerType managerType;

    @Column(name = "hub_id", updatable = false)
    private UUID hubId;

    @Column(name = "last_assigned_sequence")
    private Integer lastAssignedSequence;

    /**
     * 담당자 등록과 자동 배정이 함께 잠글 수 있는 그룹별 Cursor를 생성한다.
     */
    public static DeliveryManagerAssignmentCursor create(
            DeliveryManagerAssignmentGroup assignmentGroup
    ) {
        if (assignmentGroup == null) {
            throw new IllegalArgumentException("배정 그룹은 필수입니다.");
        }

        DeliveryManagerAssignmentCursor cursor = new DeliveryManagerAssignmentCursor();
        cursor.cursorId = UUID.randomUUID();
        cursor.assignmentGroupKey = assignmentGroup.assignmentGroupKey();
        cursor.managerType = assignmentGroup.managerType();
        cursor.hubId = assignmentGroup.hubId();
        return cursor;
    }

    /**
     * 활성 순번 목록에서 마지막 배정 다음 값을 고르고, 없으면 최소 순번으로 되돌린다.
     */
    public int assignNext(List<Integer> activeSequences) {
        if (activeSequences == null || activeSequences.isEmpty()) {
            throw new DeliveryException(DeliveryErrorCode.DELIVERY_MANAGER_UNAVAILABLE);
        }

        int minimumSequence = activeSequences.stream()
                .mapToInt(Integer::intValue)
                .min()
                .orElseThrow(() -> new DeliveryException(
                        DeliveryErrorCode.DELIVERY_MANAGER_UNAVAILABLE
                ));

        int selectedSequence = lastAssignedSequence == null
                ? minimumSequence
                : activeSequences.stream()
                        .mapToInt(Integer::intValue)
                        .filter(sequence -> sequence > lastAssignedSequence)
                        .min()
                        .orElse(minimumSequence);

        lastAssignedSequence = selectedSequence;
        return selectedSequence;
    }
}
