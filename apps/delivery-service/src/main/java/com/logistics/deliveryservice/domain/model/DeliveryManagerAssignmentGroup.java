package com.logistics.deliveryservice.domain.model;

import com.logistics.deliveryservice.domain.exception.DeliveryErrorCode;
import com.logistics.deliveryservice.domain.exception.DeliveryException;
import java.util.Collection;
import java.util.UUID;

/**
 * 담당자 유형과 허브를 하나의 배정 그룹으로 묶어 그룹 키와 순번 규칙을 관리한다.
 */
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

    // 배송 순번 검증
    public static void validateSequence(Integer sequence) {
        if (sequence == null || sequence < MIN_SEQUENCE || sequence > MAX_SEQUENCE) {
            throw invalidManagerChange();
        }
    }

    // managerType과 hubId가 비어 있지 않은지 검사
    private static void validateGroup(DeliveryManagerType managerType, UUID hubId) {
        if (managerType == null
                || hubId == null) {
            throw new DeliveryException(DeliveryErrorCode.INVALID_DELIVERY_MANAGER_HUB);
        }
    }

    private static DeliveryException invalidManagerChange() {
        return new DeliveryException(DeliveryErrorCode.INVALID_DELIVERY_MANAGER_CHANGE);
    }


    public static DeliveryManagerAssignmentGroup hubDelivery(UUID hubId) {
        return new DeliveryManagerAssignmentGroup(DeliveryManagerType.HUB_DELIVERY, hubId);
    }

    public static DeliveryManagerAssignmentGroup companyDelivery(UUID hubId) {
        return new DeliveryManagerAssignmentGroup(DeliveryManagerType.COMPANY_DELIVERY, hubId);
    }

    public String assignmentGroupKey() {
        if (managerType == DeliveryManagerType.HUB_DELIVERY) {
            return HUB_DELIVERY_GROUP_KEY;
        }
        return managerType.name() + ":" + hubId;
    }

    public int findSmallestAvailableSequence(Collection<Integer> activeSequences) {
        if (activeSequences == null) {
            throw invalidManagerChange();
        }

        boolean[] occupiedSequences = new boolean[MAX_MANAGER_COUNT];
        for (Integer sequence : activeSequences) {
            validateSequence(sequence);
            occupiedSequences[sequence] = true;
        }

        // 순번 데이터가 중복된 비정상 상황에서도 활성 인원 10명 제한을 우회할 수 없게 한다.
        if (activeSequences.size() >= MAX_MANAGER_COUNT) {
            throw new DeliveryException(DeliveryErrorCode.DELIVERY_MANAGER_GROUP_FULL);
        }

        for (int sequence = MIN_SEQUENCE; sequence <= MAX_SEQUENCE; sequence++) {
            if (!occupiedSequences[sequence]) {
                return sequence;
            }
        }
        throw new DeliveryException(DeliveryErrorCode.DELIVERY_MANAGER_GROUP_FULL);
    }
}
