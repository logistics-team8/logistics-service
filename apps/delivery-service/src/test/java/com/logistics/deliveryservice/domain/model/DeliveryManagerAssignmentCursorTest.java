package com.logistics.deliveryservice.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class DeliveryManagerAssignmentCursorTest {

    @Test
    void createsCursorForAssignmentGroupBeforeFirstAssignment() {
        UUID hubId = UUID.fromString("73d7e37c-bdd5-4334-bec1-ae42297143db");
        DeliveryManagerAssignmentGroup group =
                DeliveryManagerAssignmentGroup.companyDelivery(hubId);

        DeliveryManagerAssignmentCursor cursor =
                DeliveryManagerAssignmentCursor.create(group);

        assertThat(cursor.getCursorId()).isNotNull();
        assertThat(cursor.getAssignmentGroupKey())
                .isEqualTo("COMPANY_DELIVERY:" + hubId);
        assertThat(cursor.getManagerType())
                .isEqualTo(DeliveryManagerType.COMPANY_DELIVERY);
        assertThat(cursor.getHubId()).isEqualTo(hubId);
        assertThat(cursor.getLastAssignedSequence()).isNull();
    }
}
