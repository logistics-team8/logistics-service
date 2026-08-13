package com.logistics.deliveryservice.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.logistics.deliveryservice.domain.exception.DeliveryErrorCode;
import com.logistics.deliveryservice.domain.exception.DeliveryException;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DeliveryManagerTest {

    private static final UUID USER_ID =
            UUID.fromString("0f078b2a-61d2-42a4-bbf8-301208360a63");
    private static final UUID HUB_ID =
            UUID.fromString("b344497b-5e61-4099-958b-dcf485648da5");

    @Test
    void createsHubDeliveryManagerInHubGroup() {
        DeliveryManager deliveryManager = DeliveryManager.create(
                USER_ID,
                DeliveryManagerType.HUB_DELIVERY,
                HUB_ID,
                0
        );

        assertThat(deliveryManager.getUserId()).isEqualTo(USER_ID);
        assertThat(deliveryManager.getManagerType())
                .isEqualTo(DeliveryManagerType.HUB_DELIVERY);
        assertThat(deliveryManager.getHubId()).isEqualTo(HUB_ID);
        assertThat(deliveryManager.getDeliverySequence()).isZero();
    }

    @Test
    void createsCompanyDeliveryManagerInHubGroup() {
        DeliveryManager deliveryManager = DeliveryManager.create(
                USER_ID,
                DeliveryManagerType.COMPANY_DELIVERY,
                HUB_ID,
                9
        );

        assertThat(deliveryManager.getManagerType())
                .isEqualTo(DeliveryManagerType.COMPANY_DELIVERY);
        assertThat(deliveryManager.getHubId()).isEqualTo(HUB_ID);
        assertThat(deliveryManager.getDeliverySequence()).isEqualTo(9);
    }

    @Test
    void rejectsMissingUserSnapshot() {
        assertThatThrownBy(() -> DeliveryManager.create(
                null,
                DeliveryManagerType.HUB_DELIVERY,
                HUB_ID,
                0
        ))
                .isInstanceOf(DeliveryException.class)
                .extracting(exception -> ((DeliveryException) exception).getErrorCode())
                .isEqualTo(DeliveryErrorCode.INVALID_DELIVERY_MANAGER_USER);
    }

    @Test
    void rejectsSequenceOutsideAssignmentRange() {
        assertThatThrownBy(() -> DeliveryManager.create(
                USER_ID,
                DeliveryManagerType.HUB_DELIVERY,
                HUB_ID,
                -1
        ))
                .isInstanceOf(DeliveryException.class)
                .extracting(exception -> ((DeliveryException) exception).getErrorCode())
                .isEqualTo(DeliveryErrorCode.INVALID_DELIVERY_MANAGER_CHANGE);
    }
}
