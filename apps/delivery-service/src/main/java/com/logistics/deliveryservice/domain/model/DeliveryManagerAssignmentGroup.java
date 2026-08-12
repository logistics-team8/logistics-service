package com.logistics.deliveryservice.domain.model;

import com.logistics.deliveryservice.domain.exception.DeliveryErrorCode;
import com.logistics.deliveryservice.domain.exception.DeliveryException;
import java.util.UUID;

public record DeliveryManagerAssignmentGroup(
        DeliveryManagerType managerType,
        UUID hubId
) {

    public static final int MIN_SEQUENCE = 0;
    public static final int MAX_SEQUENCE = 9;
    public static final int MAX_MANAGER_COUNT = 10;

    private static final String HUB_DELIVERY_GROUP_KEY = "HUB_DELIVERY:GLOBAL";

    public DeliveryManagerAssignmentGroup {
        validateGroup(managerType, hubId);
    }

    // 배송 순번이 비즈니스 규칙에 맞는지 검증 (null X, 0~9)
    public static void validateSequence(Integer sequence) {
        if (sequence == null || sequence < MIN_SEQUENCE || sequence > MAX_SEQUENCE) {
            throw invalidManagerChange();
        }
    }

    // 데이터의 무결성(null X)
    private static void validateGroup(DeliveryManagerType managerType, UUID hubId) {
        if (managerType == null || hubId == null) {
            throw new DeliveryException(DeliveryErrorCode.INVALID_DELIVERY_MANAGER_HUB);
        }
    }

    // 잘못된 담당자 요청
    private static DeliveryException invalidManagerChange() {
        return new DeliveryException(DeliveryErrorCode.INVALID_DELIVERY_MANAGER_CHANGE);
    }

    // 허브 배송원 그룹 생성
    public static DeliveryManagerAssignmentGroup hubDelivery(UUID hubId) {
        return new DeliveryManagerAssignmentGroup(DeliveryManagerType.HUB_DELIVERY, hubId);
    }
    // 업체 배송원 그룹 생성
    public static DeliveryManagerAssignmentGroup companyDelivery(UUID hubId) {
        return new DeliveryManagerAssignmentGroup(DeliveryManagerType.COMPANY_DELIVERY, hubId);
    }

    public String assignmentGroupKey() {
        // 허브 배송원일때 HUB_DELIVERY_GROUP_KEY 전역 고정키 반환
        if (managerType == DeliveryManagerType.HUB_DELIVERY) {
            return HUB_DELIVERY_GROUP_KEY;
        }
        // 아니면(업체 배송원) 허브별 고유키 반환
        return managerType.name() + ":" + hubId;
    }
}
