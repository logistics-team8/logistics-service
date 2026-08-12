package com.logistics.deliveryservice.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.logistics.deliveryservice.domain.exception.DeliveryErrorCode;
import com.logistics.deliveryservice.domain.exception.DeliveryException;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DeliveryManagerAssignmentCursorTest {

    private static final UUID HUB_ID =
            UUID.fromString("73d7e37c-bdd5-4334-bec1-ae42297143db");

    @Test
    void createsCursorForAssignmentGroupBeforeFirstAssignment() {
        DeliveryManagerAssignmentGroup group =
                DeliveryManagerAssignmentGroup.companyDelivery(HUB_ID);

        DeliveryManagerAssignmentCursor cursor =
                DeliveryManagerAssignmentCursor.create(group);

        assertThat(cursor.getCursorId()).isNotNull();
        assertThat(cursor.getAssignmentGroupKey())
                .isEqualTo("COMPANY_DELIVERY:" + HUB_ID);
        assertThat(cursor.getManagerType())
                .isEqualTo(DeliveryManagerType.COMPANY_DELIVERY);
        assertThat(cursor.getHubId()).isEqualTo(HUB_ID);
        assertThat(cursor.getLastAssignedSequence()).isNull();
    }

    @Test
    void assignsSmallestSequenceOnFirstAssignment() {
        DeliveryManagerAssignmentCursor cursor = cursor();

        int assigned = cursor.assignNext(List.of(5, 0, 2));

        assertThat(assigned).isZero();
        assertThat(cursor.getLastAssignedSequence()).isZero();
    }

    @Test
    void advancesToNextGreaterActiveSequence() {
        DeliveryManagerAssignmentCursor cursor = cursor();
        cursor.assignNext(List.of(0, 1, 2));

        int assigned = cursor.assignNext(List.of(0, 1, 2));

        assertThat(assigned).isEqualTo(1);
        assertThat(cursor.getLastAssignedSequence()).isEqualTo(1);
    }

    @Test
    void wrapsToSmallestSequenceAfterLast() {
        DeliveryManagerAssignmentCursor cursor = cursor();
        cursor.assignNext(List.of(0, 2, 5));
        cursor.assignNext(List.of(0, 2, 5));
        cursor.assignNext(List.of(0, 2, 5));

        int assigned = cursor.assignNext(List.of(0, 2, 5));

        assertThat(assigned).isZero();
        assertThat(cursor.getLastAssignedSequence()).isZero();
    }

    @Test
    void skipsDeletedSequencesBetweenAssignments() {
        DeliveryManagerAssignmentCursor cursor = cursor();
        cursor.assignNext(List.of(0, 1, 2));
        cursor.assignNext(List.of(0, 1, 2));

        int assigned = cursor.assignNext(List.of(0, 2));

        assertThat(assigned).isEqualTo(2);
        assertThat(cursor.getLastAssignedSequence()).isEqualTo(2);
    }

    @Test
    void rejectsEmptyActiveSequences() {
        DeliveryManagerAssignmentCursor cursor = cursor();

        assertThatThrownBy(() -> cursor.assignNext(List.of()))
                .isInstanceOf(DeliveryException.class)
                .extracting(exception -> ((DeliveryException) exception).getErrorCode())
                .isEqualTo(DeliveryErrorCode.DELIVERY_MANAGER_UNAVAILABLE);

        assertThatThrownBy(() -> cursor.assignNext(null))
                .isInstanceOf(DeliveryException.class)
                .extracting(exception -> ((DeliveryException) exception).getErrorCode())
                .isEqualTo(DeliveryErrorCode.DELIVERY_MANAGER_UNAVAILABLE);
        assertThat(cursor.getLastAssignedSequence()).isNull();
    }

    private DeliveryManagerAssignmentCursor cursor() {
        return DeliveryManagerAssignmentCursor.create(
                DeliveryManagerAssignmentGroup.companyDelivery(HUB_ID)
        );
    }
}
