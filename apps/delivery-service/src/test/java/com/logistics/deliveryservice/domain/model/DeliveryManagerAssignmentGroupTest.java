package com.logistics.deliveryservice.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.logistics.deliveryservice.domain.exception.DeliveryErrorCode;
import com.logistics.deliveryservice.domain.exception.DeliveryException;
import java.util.List;
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

    @Test
    void selectsSmallestUnusedSequence() {
        DeliveryManagerAssignmentGroup group = DeliveryManagerAssignmentGroup.hubDelivery(HUB_ID);

        assertThat(group.findSmallestAvailableSequence(List.of(0, 2, 3))).isEqualTo(1);
    }

    @Test
    void reusesSequenceMissingFromActiveManagers() {
        DeliveryManagerAssignmentGroup group =
                DeliveryManagerAssignmentGroup.companyDelivery(HUB_ID);

        assertThat(group.findSmallestAvailableSequence(List.of(1, 2, 3))).isZero();
    }

    @Test
    void rejectsGroupWithTenActiveSequences() {
        DeliveryManagerAssignmentGroup group = DeliveryManagerAssignmentGroup.hubDelivery(HUB_ID);

        assertThatThrownBy(() -> group.findSmallestAvailableSequence(
                List.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9)
        ))
                .isInstanceOf(DeliveryException.class)
                .extracting(exception -> ((DeliveryException) exception).getErrorCode())
                .isEqualTo(DeliveryErrorCode.DELIVERY_MANAGER_GROUP_FULL);
    }

    @Test
    void enforcesTenManagerLimitEvenIfStoredSequencesAreDuplicated() {
        DeliveryManagerAssignmentGroup group = DeliveryManagerAssignmentGroup.hubDelivery(HUB_ID);

        assertThatThrownBy(() -> group.findSmallestAvailableSequence(
                List.of(0, 0, 1, 1, 2, 2, 3, 3, 4, 4)
        ))
                .isInstanceOf(DeliveryException.class)
                .extracting(exception -> ((DeliveryException) exception).getErrorCode())
                .isEqualTo(DeliveryErrorCode.DELIVERY_MANAGER_GROUP_FULL);
    }

    @Test
    void rejectsSequenceOutsideZeroToNine() {
        DeliveryManagerAssignmentGroup group = DeliveryManagerAssignmentGroup.hubDelivery(HUB_ID);

        assertThatThrownBy(() -> group.findSmallestAvailableSequence(List.of(10)))
                .isInstanceOf(DeliveryException.class)
                .extracting(exception -> ((DeliveryException) exception).getErrorCode())
                .isEqualTo(DeliveryErrorCode.INVALID_DELIVERY_MANAGER_CHANGE);
    }
}
