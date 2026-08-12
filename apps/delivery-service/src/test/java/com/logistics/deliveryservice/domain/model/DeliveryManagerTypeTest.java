package com.logistics.deliveryservice.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DeliveryManagerTypeTest {

    @Test
    void definesHubAndCompanyDeliveryManagerTypes() {
        assertThat(DeliveryManagerType.values())
                .containsExactly(
                        DeliveryManagerType.HUB_DELIVERY,
                        DeliveryManagerType.COMPANY_DELIVERY
                );
    }
}
