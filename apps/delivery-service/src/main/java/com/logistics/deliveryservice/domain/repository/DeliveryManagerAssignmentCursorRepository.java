package com.logistics.deliveryservice.domain.repository;

import com.logistics.deliveryservice.domain.model.DeliveryManagerAssignmentCursor;
import com.logistics.deliveryservice.domain.model.DeliveryManagerAssignmentGroup;

/**
 * 같은 배정 그룹에서 순번을 동시에 선택하지 않도록 Cursor 잠금을 제공한다.
 */
public interface DeliveryManagerAssignmentCursorRepository {

    DeliveryManagerAssignmentCursor acquireForUpdate(
            DeliveryManagerAssignmentGroup assignmentGroup
    );
}
