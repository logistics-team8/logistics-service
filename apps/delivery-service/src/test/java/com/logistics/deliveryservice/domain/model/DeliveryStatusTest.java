package com.logistics.deliveryservice.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DeliveryStatusTest {

    @Test
    void definesDeliveryCrudV1StatusesInProgressOrder() {
        assertThat(DeliveryStatus.values()).containsExactly(
                DeliveryStatus.HUB_WAITING,
                DeliveryStatus.HUB_MOVING,
                DeliveryStatus.DEST_HUB_ARRIVED,
                DeliveryStatus.COMPANY_MOVING,
                DeliveryStatus.COMPLETED,
                DeliveryStatus.CANCELED
        );
    }
}
