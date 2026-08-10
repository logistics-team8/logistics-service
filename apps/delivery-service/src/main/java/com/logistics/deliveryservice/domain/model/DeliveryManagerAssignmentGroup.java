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

    public static DeliveryManagerAssignmentGroup hubDelivery() {
        return new DeliveryManagerAssignmentGroup(DeliveryManagerType.HUB_DELIVERY, null);
    }

    public static DeliveryManagerAssignmentGroup companyDelivery(UUID hubId) {
        return new DeliveryManagerAssignmentGroup(DeliveryManagerType.COMPANY_DELIVERY, hubId);
    }

    /**
     * Cursor와 담당자 조회가 같은 그룹을 식별하도록 규격화된 키를 반환한다.
     */
    public String assignmentGroupKey() {
        if (managerType == DeliveryManagerType.HUB_DELIVERY) {
            return HUB_DELIVERY_GROUP_KEY;
        }
        return managerType.name() + ":" + hubId;
    }

    /**
     * 활성 담당자가 사용 중인 순번을 제외하고 0부터 가장 작은 빈 순번을 선택한다.
     */
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

    public static void validateSequence(Integer sequence) {
        if (sequence == null || sequence < MIN_SEQUENCE || sequence > MAX_SEQUENCE) {
            throw invalidManagerChange();
        }
    }

    private static void validateGroup(DeliveryManagerType managerType, UUID hubId) {
        if (managerType == null
                || (managerType == DeliveryManagerType.HUB_DELIVERY && hubId != null)
                || (managerType == DeliveryManagerType.COMPANY_DELIVERY && hubId == null)) {
            throw new DeliveryException(DeliveryErrorCode.INVALID_DELIVERY_MANAGER_HUB);
        }
    }

    private static DeliveryException invalidManagerChange() {
        return new DeliveryException(DeliveryErrorCode.INVALID_DELIVERY_MANAGER_CHANGE);
    }
}
