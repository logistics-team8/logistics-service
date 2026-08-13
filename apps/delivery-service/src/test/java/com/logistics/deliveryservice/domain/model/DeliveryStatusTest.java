package com.logistics.deliveryservice.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DeliveryStatusTest {

    @Test
    void definesDeliveryCrudV1StatusesInProgressOrder() {
        assertThat(DeliveryStatus.values()).containsExactly(
                DeliveryStatus.HUB_WAIT,
                DeliveryStatus.HUB_MOVING,
                DeliveryStatus.HUB_ARRIVED,
                DeliveryStatus.IN_DELIVERY,
                DeliveryStatus.DELIVERED,
                DeliveryStatus.FAILED,
                DeliveryStatus.CANCELED
        );
    }
}
