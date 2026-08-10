package com.logistics.deliveryservice.domain.model;

import com.logistics.deliveryservice.domain.common.BaseEntity;
import com.logistics.deliveryservice.domain.exception.DeliveryErrorCode;
import com.logistics.deliveryservice.domain.exception.DeliveryException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * User Service의 배송 담당자 정보를 배정에 필요한 형태로 보관하는 도메인 Entity다.
 */
@Getter
@Entity
@Table(
        name = "p_delivery_managers",
        indexes = @Index(
                name = "idx_delivery_managers_group_sequence",
                columnList = "manager_type, hub_id, delivery_sequence"
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeliveryManager extends BaseEntity {

    @Id
    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "hub_id")
    private UUID hubId;

    @Column(name = "slack_id", nullable = false)
    private String slackId;

    @Enumerated(EnumType.STRING)
    @Column(name = "manager_type", nullable = false, length = 30)
    private DeliveryManagerType managerType;

    @Column(name = "delivery_sequence", nullable = false)
    private Integer deliverySequence;

    /**
     * 검증된 사용자 스냅샷과 잠금 안에서 선택한 순번으로 신규 담당자를 생성한다.
     */
    public static DeliveryManager create(
            UUID userId,
            String slackId,
            DeliveryManagerAssignmentGroup assignmentGroup,
            Integer deliverySequence
    ) {
        if (userId == null || slackId == null || slackId.isBlank()) {
            throw new DeliveryException(DeliveryErrorCode.INVALID_DELIVERY_MANAGER_USER);
        }
        if (assignmentGroup == null) {
            throw new DeliveryException(DeliveryErrorCode.INVALID_DELIVERY_MANAGER_HUB);
        }
        DeliveryManagerAssignmentGroup.validateSequence(deliverySequence);

        DeliveryManager deliveryManager = new DeliveryManager();
        deliveryManager.userId = userId;
        deliveryManager.hubId = assignmentGroup.hubId();
        deliveryManager.slackId = slackId;
        deliveryManager.managerType = assignmentGroup.managerType();
        deliveryManager.deliverySequence = deliverySequence;
        return deliveryManager;
    }
}
