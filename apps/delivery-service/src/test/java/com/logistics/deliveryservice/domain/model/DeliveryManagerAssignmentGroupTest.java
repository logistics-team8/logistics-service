package com.logistics.deliveryservice.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.logistics.deliveryservice.domain.exception.DeliveryErrorCode;
import com.logistics.deliveryservice.domain.exception.DeliveryException;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DeliveryManagerAssignmentGroupTest {

    private static final UUID HUB_ID =
            UUID.fromString("848c1286-cea6-4b03-8a19-d91f8dddb078");

    @Test
    void createsHubDeliveryGroupForHub() {
        DeliveryManagerAssignmentGroup group = DeliveryManagerAssignmentGroup.hubDelivery(HUB_ID);

        assertThat(group.managerType()).isEqualTo(DeliveryManagerType.HUB_DELIVERY);
        assertThat(group.hubId()).isEqualTo(HUB_ID);
        assertThat(group.assignmentGroupKey()).isEqualTo("HUB_DELIVERY:GLOBAL");
    }

    @Test
    void createsCompanyDeliveryGroupForHub() {
        DeliveryManagerAssignmentGroup group =
                DeliveryManagerAssignmentGroup.companyDelivery(HUB_ID);

        assertThat(group.managerType()).isEqualTo(DeliveryManagerType.COMPANY_DELIVERY);
        assertThat(group.hubId()).isEqualTo(HUB_ID);
        assertThat(group.assignmentGroupKey()).isEqualTo("COMPANY_DELIVERY:" + HUB_ID);
    }

    @Test
    void rejectsMissingHubId() {
        assertThatThrownBy(() -> new DeliveryManagerAssignmentGroup(
                DeliveryManagerType.HUB_DELIVERY,
                null
        ))
                .isInstanceOf(DeliveryException.class)
                .extracting(exception -> ((DeliveryException) exception).getErrorCode())
                .isEqualTo(DeliveryErrorCode.INVALID_DELIVERY_MANAGER_HUB);

        assertThatThrownBy(() -> DeliveryManagerAssignmentGroup.companyDelivery(null))
                .isInstanceOf(DeliveryException.class)
                .extracting(exception -> ((DeliveryException) exception).getErrorCode())
                .isEqualTo(DeliveryErrorCode.INVALID_DELIVERY_MANAGER_HUB);
    }

}
