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

    // Slack은 User Service에서 조회


    public static DeliveryManager create(
            UUID userId,
            DeliveryManagerAssignmentGroup assignmentGroup, //배송 담당자 배정 그룹
            Integer deliverySequence
    ) {
        if (userId == null) {
            throw new DeliveryException(DeliveryErrorCode.INVALID_DELIVERY_MANAGER_USER);
        }
        if (assignmentGroup == null) {
            throw new DeliveryException(DeliveryErrorCode.INVALID_DELIVERY_MANAGER_HUB);
        }

        //전달받은 순번이 0~9안에 있는지 검사
        DeliveryManagerAssignmentGroup.validateSequence(deliverySequence);

        DeliveryManager deliveryManager = new DeliveryManager();
        deliveryManager.userId = userId;
        deliveryManager.hubId = assignmentGroup.hubId();
        deliveryManager.managerType = assignmentGroup.managerType();
        deliveryManager.deliverySequence = deliverySequence;
        return deliveryManager;
    }
}
