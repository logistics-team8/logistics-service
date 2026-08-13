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
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "p_delivery_managers")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeliveryManager extends BaseEntity {

    @Id
    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "hub_id", nullable = false)
    private UUID hubId;

    @Enumerated(EnumType.STRING)
    @Column(name = "manager_type", nullable = false, length = 30)
    private DeliveryManagerType managerType;

    @Column(name = "delivery_sequence", nullable = false)
    private Integer deliverySequence;

    public static DeliveryManager create(
            UUID userId,
            DeliveryManagerType managerType,
            UUID hubId,
            Integer deliverySequence
    ) {
        if (userId == null) {
            throw new DeliveryException(DeliveryErrorCode.INVALID_DELIVERY_MANAGER_USER);
        }

        // managerType나 hubId 둘 다 비어있지 않는지 검사
        validateManagerFields(managerType, hubId);

        // 전달받은 순번이 0~9안에 있는지 검사
        DeliveryManagerAssignmentGroup.validateSequence(deliverySequence);

        DeliveryManager deliveryManager = new DeliveryManager();
        deliveryManager.userId = userId;
        deliveryManager.hubId = hubId;
        deliveryManager.managerType = managerType;
        deliveryManager.deliverySequence = deliverySequence;
        return deliveryManager;
    }

    public void update(
            DeliveryManagerType managerType,
            UUID hubId,
            Integer deliverySequence
    ) {
        validateManagerFields(managerType, hubId);
        DeliveryManagerAssignmentGroup.validateSequence(deliverySequence);

        this.managerType = managerType;
        this.hubId = hubId;
        this.deliverySequence = deliverySequence;
    }

    // Soft Delete(논리 삭제)
    public void delete(UUID deletedBy) {
        markDeleted(deletedBy);
    }

    // managerType나 hubId 둘 다 비어있지 않는지 검증
    private static void validateManagerFields(DeliveryManagerType managerType, UUID hubId) {
        if (managerType == null || hubId == null) {
            throw new DeliveryException(DeliveryErrorCode.INVALID_DELIVERY_MANAGER_HUB);
        }
    }
}
